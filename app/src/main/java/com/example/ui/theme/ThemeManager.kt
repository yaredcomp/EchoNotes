package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

object ThemeManager {
    private const val PREFS_NAME = "echonotes_theme_prefs"
    private const val KEY_DARK_THEME = "is_dark_theme"

    private var prefs: SharedPreferences? = null

    private val _isDarkTheme = mutableStateOf(true)
    val isDarkTheme: Boolean
        get() = _isDarkTheme.value

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _isDarkTheme.value = prefs?.getBoolean(KEY_DARK_THEME, true) ?: true
        }
    }

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        _isDarkTheme.value = newTheme
        prefs?.edit()?.putBoolean(KEY_DARK_THEME, newTheme)?.apply()
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs?.edit()?.putBoolean(KEY_DARK_THEME, isDark)?.apply()
    }
}
