package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GroqClient
import com.example.ai.GroqResult
import com.example.command.ActionDispatcher
import com.example.command.ActionResult
import com.example.command.ActionType
import com.example.command.CommandParser
import com.example.command.ParsedCommand
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.repository.AssistantRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.SettingsRepository
import com.example.service.EzeForegroundVoiceService
import com.example.ui.components.OrbState
import com.example.voice.SpeechRecognizerHelper
import com.example.voice.TtsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val settingsRepository = SettingsRepository(application)
    val memoryRepository = MemoryRepository(db.memoryDao())
    val groqClient = GroqClient(application)
    val assistantRepository = AssistantRepository(
        db.chatMessageDao(),
        memoryRepository,
        settingsRepository,
        groqClient
    )
    private val actionDispatcher = ActionDispatcher(application)

    // UI States
    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _statusText = MutableStateFlow("Tap orb or mic to speak")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isFriendMode = MutableStateFlow(settingsRepository.isFriendMode())
    val isFriendMode: StateFlow<Boolean> = _isFriendMode.asStateFlow()

    private val _isWakeWordEnabled = MutableStateFlow(settingsRepository.isWakeWordEnabled())
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private val _activeVoiceLevel = MutableStateFlow(0f)
    val activeVoiceLevel: StateFlow<Float> = _activeVoiceLevel.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = assistantRepository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val memories: StateFlow<List<MemoryEntity>> = memoryRepository.allMemories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Speech & TTS
    private var ttsHelper: TtsHelper? = null
    private var speechRecognizerHelper: SpeechRecognizerHelper? = null

    init {
        ttsHelper = TtsHelper(application) { isSpeaking ->
            if (isSpeaking) {
                _orbState.value = OrbState.SPEAKING
                _statusText.value = "EZE is speaking..."
            } else {
                if (_orbState.value == OrbState.SPEAKING) {
                    _orbState.value = OrbState.IDLE
                    _statusText.value = "Ready • Tap orb or mic"
                }
            }
        }.apply {
            setSpeechRate(settingsRepository.getSpeechRate())
            setPitch(settingsRepository.getSpeechPitch())
        }

        speechRecognizerHelper = SpeechRecognizerHelper(
            context = application,
            onListeningStateChanged = { listening ->
                _isListening.value = listening
                if (listening) {
                    _orbState.value = OrbState.LISTENING
                    _statusText.value = "Listening to you..."
                } else if (_orbState.value == OrbState.LISTENING) {
                    _orbState.value = OrbState.IDLE
                    _statusText.value = "Ready • Tap orb or mic"
                }
            },
            onResult = { text ->
                processUserQuery(text, fromVoice = true)
            },
            onError = { error ->
                _orbState.value = OrbState.ERROR
                _statusText.value = error
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    if (_orbState.value == OrbState.ERROR) {
                        _orbState.value = OrbState.IDLE
                        _statusText.value = "Ready • Tap orb or mic"
                    }
                }
            },
            onRmsChanged = { rms ->
                _activeVoiceLevel.value = rms
            }
        )
    }

    fun toggleListening() {
        if (_isListening.value) {
            speechRecognizerHelper?.stopListening()
        } else {
            ttsHelper?.stop()
            speechRecognizerHelper?.startListening()
        }
    }

    fun stopSpeaking() {
        ttsHelper?.stop()
        if (_orbState.value == OrbState.SPEAKING) {
            _orbState.value = OrbState.IDLE
            _statusText.value = "Ready • Tap orb or mic"
        }
    }

    fun speakText(text: String) {
        ttsHelper?.speak(text)
    }

    fun setFriendMode(enabled: Boolean) {
        _isFriendMode.value = enabled
        settingsRepository.setFriendMode(enabled)
        val mode = if (enabled) "AI Friend & Companion mode" else "Smart Assistant mode"
        _statusText.value = "$mode active"
    }

    fun toggleWakeWord(enabled: Boolean) {
        _isWakeWordEnabled.value = enabled
        settingsRepository.setWakeWordEnabled(enabled)
        if (enabled) {
            EzeForegroundVoiceService.start(getApplication())
        } else {
            EzeForegroundVoiceService.stop(getApplication())
        }
    }

    fun processUserQuery(query: String, fromVoice: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        ttsHelper?.stop()

        viewModelScope.launch {
            // 1. Record user query in conversation history
            assistantRepository.saveMessage(
                role = "user",
                content = trimmed,
                isFriendMode = _isFriendMode.value
            )

            _orbState.value = OrbState.THINKING
            _statusText.value = "EZE is thinking..."

            // 2. Parse command locally
            val command = CommandParser.parse(trimmed)

            when (command.actionType) {
                ActionType.SAVE_MEMORY -> {
                    val fact = command.parameters["fact"] ?: trimmed
                    memoryRepository.saveMemory("Preference", fact, "preference")
                    val response = "Saved to your memory: \"$fact\"."
                    saveAndRespond(response, "Memory Saved", fromVoice)
                }
                ActionType.CLEAR_MEMORY -> {
                    memoryRepository.clearAll()
                    saveAndRespond("All stored memories have been cleared.", "Memory Cleared", fromVoice)
                }
                ActionType.CALCULATOR,
                ActionType.TOGGLE_FLASHLIGHT,
                ActionType.SET_ALARM,
                ActionType.SHOW_ALARMS,
                ActionType.SET_TIMER,
                ActionType.SET_REMINDER,
                ActionType.OPEN_DIALER,
                ActionType.CALL_PHONE,
                ActionType.OPEN_CONTACTS,
                ActionType.OPEN_CALL_LOG,
                ActionType.COMPOSE_SMS,
                ActionType.OPEN_APP,
                ActionType.SET_VOLUME,
                ActionType.MEDIA_PLAY_PAUSE,
                ActionType.MEDIA_NEXT,
                ActionType.MEDIA_PREVIOUS,
                ActionType.OPEN_SETTINGS,
                ActionType.OPEN_WIFI_SETTINGS,
                ActionType.OPEN_BLUETOOTH_SETTINGS,
                ActionType.OPEN_BATTERY_SETTINGS,
                ActionType.OPEN_DISPLAY_SETTINGS,
                ActionType.OPEN_SOUND_SETTINGS,
                ActionType.OPEN_ASSISTANT_SETTINGS,
                ActionType.OPEN_MAPS,
                ActionType.NAVIGATE_TO,
                ActionType.READ_NOTIFICATIONS -> {
                    // Real phone action execution
                    executeDeviceAction(command, fromVoice)
                }
                ActionType.WEB_SEARCH -> {
                    val webRes = actionDispatcher.execute(command)
                    val reply = when (webRes) {
                        is ActionResult.Success -> webRes.message
                        is ActionResult.Failed -> webRes.reason
                        else -> "Opening web search."
                    }
                    saveAndRespond(reply, "Web Search", fromVoice)
                }
                ActionType.AI_QUERY, ActionType.UNKNOWN -> {
                    executeAiQuery(trimmed, fromVoice)
                }
            }
        }
    }

    private suspend fun executeDeviceAction(command: ParsedCommand, fromVoice: Boolean) {
        val result = withContext(Dispatchers.Main) {
            actionDispatcher.execute(command)
        }

        val responseText = when (result) {
            is ActionResult.Success -> result.message
            is ActionResult.Failed -> "Unable to complete action: ${result.reason}"
            is ActionResult.PermissionNeeded -> "Permission required: ${result.rationale}"
            is ActionResult.SecurityRestriction -> {
                result.alternativeAction?.invoke()
                result.message
            }
        }

        saveAndRespond(responseText, command.actionType.displayName, fromVoice)
    }

    private suspend fun executeAiQuery(query: String, fromVoice: Boolean) {
        val apiKey = settingsRepository.getGroqApiKey()

        if (apiKey.isNullOrBlank()) {
            val err = "Groq API key is not configured. Please go to Settings and enter your free Groq API key."
            _orbState.value = OrbState.ERROR
            _statusText.value = "Missing Groq API Key"
            saveAndRespond(err, "Configuration Needed", fromVoice)
            return
        }

        when (val result = assistantRepository.queryGroq(query, _isFriendMode.value)) {
            is GroqResult.Success -> {
                saveAndRespond(result.text, null, fromVoice)
            }
            is GroqResult.Error -> {
                _orbState.value = OrbState.ERROR
                _statusText.value = result.message
                saveAndRespond("Error: ${result.message}", "Error", fromVoice)
            }
        }
    }

    private suspend fun saveAndRespond(text: String, actionType: String?, fromVoice: Boolean) {
        assistantRepository.saveMessage(
            role = "assistant",
            content = text,
            actionType = actionType,
            actionStatus = "SUCCESS",
            isFriendMode = _isFriendMode.value
        )

        _statusText.value = "EZE responded"

        if (fromVoice) {
            ttsHelper?.speak(text)
        } else {
            _orbState.value = OrbState.IDLE
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            assistantRepository.clearHistory()
            _statusText.value = "Chat history cleared"
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepository.clearAll()
            _statusText.value = "Memories cleared"
        }
    }

    fun saveMemoryManually(key: String, value: String) {
        viewModelScope.launch {
            memoryRepository.saveMemory(key, value)
            _statusText.value = "Memory saved"
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerHelper?.destroy()
        ttsHelper?.shutdown()
    }
}
