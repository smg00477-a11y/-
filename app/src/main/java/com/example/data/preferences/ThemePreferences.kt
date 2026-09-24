package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("raqeem_theme_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DARK_MODE = "key_dark_mode_enabled"
        // Mandatory requirement: fresh installation MUST start in Dark Mode
        private const val DEFAULT_DARK_MODE = true
    }

    private val _isDarkMode = MutableStateFlow(
        prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE)
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }
}
