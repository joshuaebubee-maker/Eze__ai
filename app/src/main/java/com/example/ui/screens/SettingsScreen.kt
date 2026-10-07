package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GroqResult
import com.example.ui.theme.EzeBackground
import com.example.ui.theme.EzeBorder
import com.example.ui.theme.EzePrimary
import com.example.ui.theme.EzeSecondary
import com.example.ui.theme.EzeSurface
import com.example.ui.theme.EzeSurfaceVariant
import com.example.ui.theme.EzeTertiary
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = viewModel.settingsRepository

    var apiKey by remember { mutableStateOf(settings.getGroqApiKey() ?: "") }
    var selectedModel by remember { mutableStateOf(settings.getGroqModel()) }
    var showPassword by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var testStatusText by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    var isWakeWordOn by remember { mutableStateOf(settings.isWakeWordEnabled()) }
    var isFriendModeOn by remember { mutableStateOf(settings.isFriendMode()) }
    var isMemoryOn by remember { mutableStateOf(settings.isMemoryEnabled()) }
    var isConciseOn by remember { mutableStateOf(settings.isConciseMode()) }
    var userName by remember { mutableStateOf(settings.getUserName()) }
    var speechSpeed by remember { mutableFloatStateOf(settings.getSpeechRate()) }

    val popularModels = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768",
        "gemma2-9b-it"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EzeBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text("Settings", fontWeight = FontWeight.Bold, color = EzePrimary)
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = EzeBackground)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // SECTION 1: GROQ AI CONFIGURATION
            SettingsCard(title = "Groq AI Configuration", icon = Icons.Default.Key) {
                Text(
                    text = "Enter your personal Groq API key to power EZE's conversational intelligence. Keys are hardware-encrypted in Android Keystore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // API Key Field
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Groq API Key") },
                    placeholder = { Text("gsk_...") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle key visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("groq_api_key_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Model Selection
                ExposedDropdownMenuBox(
                    expanded = modelDropdownExpanded,
                    onExpandedChange = { modelDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        label = { Text("Groq Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("groq_model_input")
                    )

                    ExposedDropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false }
                    ) {
                        popularModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    selectedModel = model
                                    modelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Test & Save & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isTestingConnection = true
                                testStatusText = "Testing Groq connection..."
                                val res = viewModel.groqClient.testConnection(apiKey, selectedModel)
                                isTestingConnection = false
                                testStatusText = when (res) {
                                    is GroqResult.Success -> "Connection successful! Groq is ready."
                                    is GroqResult.Error -> "Connection failed: ${res.message}"
                                }
                            }
                        },
                        enabled = !isTestingConnection && apiKey.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_groq_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Test Connection")
                        }
                    }

                    Button(
                        onClick = {
                            settings.saveGroqApiKey(apiKey)
                            settings.saveGroqModel(selectedModel)
                            Toast.makeText(context, "AI settings saved securely.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EzePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_groq_button")
                    ) {
                        Text("Save Settings", color = EzeBackground)
                    }
                }

                if (testStatusText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testStatusText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testStatusText?.contains("successful") == true) EzeTertiary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (apiKey.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            settings.clearGroqApiKey()
                            apiKey = ""
                            testStatusText = null
                            Toast.makeText(context, "API Key cleared.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("clear_key_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Stored Key")
                    }
                }
            }

            // SECTION 2: VOICE & WAKE WORD
            SettingsCard(title = "Voice & Wake Word", icon = Icons.Default.RecordVoiceOver) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wake phrase (\"Hey EZE\" / \"EZE\")", fontWeight = FontWeight.Medium)
                        Text(
                            "Listens for wake words via foreground microphone service",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isWakeWordOn,
                        onCheckedChange = { checked ->
                            isWakeWordOn = checked
                            viewModel.toggleWakeWord(checked)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EzePrimary),
                        modifier = Modifier.testTag("wake_word_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speech Speed Slider
                Text("Speech Rate: ${String.format("%.1f", speechSpeed)}x", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = speechSpeed,
                    onValueChange = {
                        speechSpeed = it
                        settings.setSpeechRate(it)
                    },
                    valueRange = 0.7f..1.6f,
                    colors = SliderDefaults.colors(thumbColor = EzePrimary, activeTrackColor = EzePrimary)
                )

                OutlinedButton(
                    onClick = { viewModel.speakText("Hello! I am EZE, your personal Android assistant.") },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Test Voice Speech")
                }
            }

            // SECTION 3: ASSISTANT PERSONALITY & MEMORY
            SettingsCard(title = "Assistant & Companion", icon = Icons.Default.Psychology) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = {
                        userName = it
                        settings.setUserName(it)
                    },
                    label = { Text("Your Preferred Name") },
                    placeholder = { Text("e.g. Joshua") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Friend & Companion Mode", fontWeight = FontWeight.Medium)
                        Text(
                            "Warm, humorous, and empathetic conversation style",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isFriendModeOn,
                        onCheckedChange = {
                            isFriendModeOn = it
                            viewModel.setFriendMode(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EzeSecondary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Concise Answers", fontWeight = FontWeight.Medium)
                        Text(
                            "Keep responses brief unless detailed explanation is asked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isConciseOn,
                        onCheckedChange = {
                            isConciseOn = it
                            settings.setConciseMode(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EzePrimary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Local Memory System", fontWeight = FontWeight.Medium)
                        Text(
                            "Remember preferences and study goals locally in Room",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isMemoryOn,
                        onCheckedChange = {
                            isMemoryOn = it
                            settings.setMemoryEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EzePrimary)
                    )
                }
            }

            // SECTION 4: SYSTEM & ASSISTANT INTEGRATION
            SettingsCard(title = "System Assistant Integration", icon = Icons.Default.Assistant) {
                Text(
                    text = "Make EZE your default device assistant to trigger it by holding the home button or navigation bar gesture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(fallback)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EzePrimary),
                    modifier = Modifier.fillMaxWidth().testTag("default_assistant_setup_button")
                ) {
                    Text("Set EZE as Default Assistant", color = EzeBackground)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EzeSurfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("notification_access_button")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = EzePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Notification Access", color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Battery optimization guidance for Android 11+
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EzeSurfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("battery_optimization_button")
                ) {
                    Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = EzeSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Battery Optimization Guidance", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // SECTION 5: PRIVACY & DATA MANAGEMENT
            SettingsCard(title = "Privacy & Local Data", icon = Icons.Default.Security) {
                Text(
                    text = "All chat history and memory items are stored strictly on your device in local SQLite/Room database. No chat history is uploaded to third-party servers except prompts sent to your configured Groq API key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearChatHistory()
                            Toast.makeText(context, "Conversations cleared.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear History")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearAllMemories()
                            Toast.makeText(context, "Memories cleared.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Memories")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = EzeSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EzeBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EzePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
