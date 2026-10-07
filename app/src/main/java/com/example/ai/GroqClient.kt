package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GroqClient(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GroqChatRequest::class.java)
    private val responseAdapter = moshi.adapter(GroqChatResponse::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun testConnection(apiKey: String, model: String): GroqResult {
        if (!isOnline()) {
            return GroqResult.Error("No internet connection. Please check your Wi-Fi or mobile data.", isNetworkError = true)
        }
        if (apiKey.isBlank()) {
            return GroqResult.Error("Groq API key is empty. Please enter your API key in Settings.")
        }

        val testMessages = listOf(
            GroqMessage(role = "user", content = "Hi, reply with OK.")
        )
        return executeChatCompletion(apiKey, model, testMessages, maxTokens = 10)
    }

    suspend fun chatCompletion(
        apiKey: String,
        model: String,
        messages: List<GroqMessage>,
        temperature: Double = 0.6,
        maxTokens: Int = 1024
    ): GroqResult {
        if (!isOnline()) {
            return GroqResult.Error("Device is offline. Internet connection is required for Groq AI.", isNetworkError = true)
        }
        if (apiKey.isBlank()) {
            return GroqResult.Error("No Groq API key configured. Please add your key in Settings.")
        }
        return executeChatCompletion(apiKey, model, messages, temperature, maxTokens)
    }

    private suspend fun executeChatCompletion(
        apiKey: String,
        model: String,
        messages: List<GroqMessage>,
        temperature: Double = 0.6,
        maxTokens: Int = 1024
    ): GroqResult = withContext(Dispatchers.IO) {
        val requestPayload = GroqChatRequest(
            model = model.ifBlank { "llama-3.3-70b-versatile" },
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens
        )

        val jsonBody = try {
            requestAdapter.toJson(requestPayload)
        } catch (e: Exception) {
            return@withContext GroqResult.Error("Failed to prepare request payload: ${e.localizedMessage}")
        }

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        var lastException: Exception? = null
        for (attempt in 1..2) {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    val responseBodyString = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val parsed = responseAdapter.fromJson(responseBodyString)
                        val text = parsed?.choices?.firstOrNull()?.message?.content?.trim()
                        if (!text.isNullOrBlank()) {
                            return@withContext GroqResult.Success(text)
                        } else {
                            return@withContext GroqResult.Error("Empty response received from Groq.")
                        }
                    } else {
                        // Handle HTTP error codes
                        val errorDetail = try {
                            val parsed = responseAdapter.fromJson(responseBodyString)
                            parsed?.error?.message
                        } catch (e: Exception) {
                            null
                        }

                        val message = when (response.code) {
                            401 -> "Invalid Groq API key (401 Unauthorized). Please check your key in Settings."
                            404 -> "Model '$model' not found or unsupported on Groq (404). Check model name."
                            429 -> "Groq rate limit exceeded (429). Please wait a moment and try again."
                            500, 502, 503, 504 -> "Groq server temporarily unavailable (${response.code})."
                            else -> errorDetail ?: "Groq API error HTTP ${response.code}: ${response.message}"
                        }
                        return@withContext GroqResult.Error(message, code = response.code)
                    }
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt == 1 && isOnline()) {
                    // Retry once on transient network drop
                    continue
                }
            } catch (e: Exception) {
                return@withContext GroqResult.Error("Network error: ${e.localizedMessage}", isNetworkError = true)
            }
        }

        GroqResult.Error(
            lastException?.localizedMessage ?: "Failed to reach Groq API. Please check your connection.",
            isNetworkError = true
        )
    }
}
