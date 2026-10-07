package com.example.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.6,
    @Json(name = "max_tokens") val maxTokens: Int = 1024
)

@JsonClass(generateAdapter = true)
data class GroqMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class GroqChatResponse(
    val id: String? = null,
    val choices: List<GroqChoice>? = null,
    val error: GroqApiError? = null
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    val index: Int? = null,
    val message: GroqMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GroqApiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

sealed class GroqResult {
    data class Success(val text: String) : GroqResult()
    data class Error(val message: String, val code: Int? = null, val isNetworkError: Boolean = false) : GroqResult()
}
