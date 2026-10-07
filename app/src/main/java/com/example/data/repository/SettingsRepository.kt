package com.example.data.repository

import android.content.Context
import com.example.data.security.KeystoreManager

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("eze_preferences", Context.MODE_PRIVATE)
    val keystoreManager = KeystoreManager(context)

    companion object {
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_FRIEND_MODE = "friend_mode"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_CONCISE_MODE = "concise_mode"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_USER_NAME = "user_name"
    }

    fun isWakeWordEnabled(): Boolean = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
    fun setWakeWordEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()

    fun isFriendMode(): Boolean = prefs.getBoolean(KEY_FRIEND_MODE, false)
    fun setFriendMode(enabled: Boolean) = prefs.edit().putBoolean(KEY_FRIEND_MODE, enabled).apply()

    fun isMemoryEnabled(): Boolean = prefs.getBoolean(KEY_MEMORY_ENABLED, true)
    fun setMemoryEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_MEMORY_ENABLED, enabled).apply()

    fun isConciseMode(): Boolean = prefs.getBoolean(KEY_CONCISE_MODE, true)
    fun setConciseMode(concise: Boolean) = prefs.edit().putBoolean(KEY_CONCISE_MODE, concise).apply()

    fun getSpeechRate(): Float = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
    fun setSpeechRate(rate: Float) = prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()

    fun getSpeechPitch(): Float = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
    fun setSpeechPitch(pitch: Float) = prefs.edit().putFloat(KEY_SPEECH_PITCH, pitch).apply()

    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    fun setOnboardingDone(done: Boolean) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun setUserName(name: String) = prefs.edit().putString(KEY_USER_NAME, name.trim()).apply()

    // Groq credentials delegated to hardware-backed Keystore
    fun getGroqApiKey(): String? = keystoreManager.getGroqApiKey()
    fun saveGroqApiKey(key: String) = keystoreManager.saveGroqApiKey(key)
    fun clearGroqApiKey() = keystoreManager.clearGroqApiKey()

    fun getGroqModel(): String = keystoreManager.getGroqModel()
    fun saveGroqModel(model: String) = keystoreManager.saveGroqModel(model)
}
