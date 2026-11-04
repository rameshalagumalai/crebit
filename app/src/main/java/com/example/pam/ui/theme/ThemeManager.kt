package com.example.pam.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val title: String) {
    SYSTEM("Device default"),
    LIGHT("Light"),
    DARK("Dark")
}

object ThemeManager {
    private const val PREFS_NAME = "pam_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    private var prefs: SharedPreferences? = null
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        val sp = prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also { prefs = it }
        val savedName = sp.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val mode = try {
            ThemeMode.valueOf(savedName)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
        _themeMode.value = mode
    }

    fun isDark(context: Context): Boolean {
        val sp = prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also { prefs = it }
        val savedName = sp.getString(KEY_THEME_MODE, _themeMode.value.name) ?: _themeMode.value.name
        val currentMode = try {
            ThemeMode.valueOf(savedName)
        } catch (_: Exception) {
            _themeMode.value
        }
        _themeMode.value = currentMode
        return when (currentMode) {
            ThemeMode.SYSTEM -> {
                val nightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }

    fun setThemeMode(mode: ThemeMode, context: Context? = null) {
        _themeMode.value = mode
        val ctx = context?.applicationContext
        val sp = prefs ?: ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.also { prefs = it }
        sp?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
        if (ctx != null) {
            try {
                com.example.pam.ui.widget.OverviewWidgetProvider.updateAllWidgets(ctx)
            } catch (_: Exception) {}
            try {
                com.example.pam.ui.widget.QuickActionsWidgetProvider.updateAllWidgets(ctx)
            } catch (_: Exception) {}
        }
    }
}
