package com.example.data.local.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user-configured preferences including custom Gemini API keys
 * and audio settings with persistent on-device SharedPreferences storage.
 */
class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "echonotes_user_prefs"
        private const val KEY_CUSTOM_GEMINI_API_KEY = "custom_gemini_api_key"
        private const val KEY_ACTIVE_GEMINI_MODEL = "active_gemini_model"

        @Volatile
        private var instance: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: UserPreferencesManager(context).also { instance = it }
            }
        }
    }

    /**
     * Retrieves the custom Gemini API key configured directly by the user in-app, if any.
     */
    fun getCustomGeminiApiKey(): String {
        return prefs.getString(KEY_CUSTOM_GEMINI_API_KEY, "")?.trim()?.trim('"', '\'') ?: ""
    }

    /**
     * Saves a custom Gemini API key entered by the user.
     */
    fun setCustomGeminiApiKey(apiKey: String) {
        val sanitized = apiKey.trim().trim('"', '\'')
        prefs.edit().putString(KEY_CUSTOM_GEMINI_API_KEY, sanitized).apply()
    }

    /**
     * Clears any custom Gemini API key so the app falls back to the .env / BuildConfig key.
     */
    fun clearCustomGeminiApiKey() {
        prefs.edit().remove(KEY_CUSTOM_GEMINI_API_KEY).apply()
    }

    /**
     * Checks if a user has entered a custom key.
     */
    fun hasCustomGeminiApiKey(): Boolean {
        return getCustomGeminiApiKey().isNotEmpty()
    }

    /**
     * Retrieves the active or cached Gemini model name.
     */
    fun getActiveGeminiModel(): String {
        return prefs.getString(KEY_ACTIVE_GEMINI_MODEL, "") ?: ""
    }

    /**
     * Saves the currently active or auto-detected Gemini model name.
     */
    fun setActiveGeminiModel(model: String) {
        prefs.edit().putString(KEY_ACTIVE_GEMINI_MODEL, model.trim()).apply()
    }

    /**
     * Resets the active Gemini model cache.
     */
    fun clearActiveGeminiModel() {
        prefs.edit().remove(KEY_ACTIVE_GEMINI_MODEL).apply()
    }
}
