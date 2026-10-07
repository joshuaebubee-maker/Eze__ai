package com.example.data.repository

import com.example.ai.GroqChatRequest
import com.example.ai.GroqClient
import com.example.ai.GroqMessage
import com.example.ai.GroqResult
import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class AssistantRepository(
    private val chatMessageDao: ChatMessageDao,
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository,
    private val groqClient: GroqClient
) {

    val allMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    suspend fun saveMessage(
        role: String,
        content: String,
        actionType: String? = null,
        actionStatus: String? = null,
        isFriendMode: Boolean = false
    ): Long {
        val entity = ChatMessageEntity(
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            actionType = actionType,
            actionStatus = actionStatus,
            isFriendMode = isFriendMode
        )
        return chatMessageDao.insertMessage(entity)
    }

    suspend fun clearHistory() {
        chatMessageDao.clearAllMessages()
    }

    suspend fun deleteMessage(id: Long) {
        chatMessageDao.deleteMessageById(id)
    }

    suspend fun searchMessages(query: String): List<ChatMessageEntity> {
        return chatMessageDao.searchMessages(query)
    }

    suspend fun queryGroq(userPrompt: String, isFriendMode: Boolean): GroqResult {
        val apiKey = settingsRepository.getGroqApiKey() ?: ""
        val model = settingsRepository.getGroqModel()

        // Fetch recent messages for conversational context
        val recentHistory = chatMessageDao.getRecentMessages(8).reversed()

        // Build prompt with memory context
        val memoryContext = if (settingsRepository.isMemoryEnabled()) {
            val memories = memoryRepository.getAllMemoriesList()
            if (memories.isNotEmpty()) {
                "User memories & preferences:\n" + memories.joinToString("\n") { "- ${it.key}: ${it.value}" }
            } else ""
        } else ""

        val systemPrompt = buildSystemPrompt(isFriendMode, memoryContext)

        val messages = mutableListOf<GroqMessage>()
        messages.add(GroqMessage(role = "system", content = systemPrompt))

        recentHistory.forEach { item ->
            val role = if (item.role == "user") "user" else "assistant"
            messages.add(GroqMessage(role = role, content = item.content))
        }

        messages.add(GroqMessage(role = "user", content = userPrompt))

        return groqClient.chatCompletion(
            apiKey = apiKey,
            model = model,
            messages = messages,
            temperature = if (isFriendMode) 0.8 else 0.5,
            maxTokens = 1024
        )
    }

    private fun buildSystemPrompt(isFriendMode: Boolean, memoryContext: String): String {
        val userName = settingsRepository.getUserName()
        val isConcise = settingsRepository.isConciseMode()

        return buildString {
            append("You are EZE, a native intelligent personal Android assistant")
            if (isFriendMode) {
                append(" and empathetic AI companion / friend.")
            } else {
                append(".")
            }
            append(" Created to be helpful, calm, smart, and genuine.\n")

            if (userName.isNotBlank()) {
                append("The user's name is $userName.\n")
            }

            if (isFriendMode) {
                append("Personality: Warm, encouraging, humorous, attentive, like a close trusted companion. Be conversational, ask thoughtful follow-ups when appropriate, and share lighthearted thoughts.\n")
            } else {
                append("Personality: Direct, polite, highly competent, natural, and efficient.\n")
            }

            if (isConcise) {
                append("Format guideline: Keep answers concise, clear, and direct unless the user asks for a detailed explanation, essay, or in-depth tutoring.\n")
            }

            append("CRITICAL: Never pretend you executed a hardware action (like setting an alarm, turning on flashlight, or placing a call) if you are just answering text. Device actions are handled natively by EZE's local action engine.\n")

            if (memoryContext.isNotBlank()) {
                append("\n$memoryContext\n")
            }
        }
    }
}
