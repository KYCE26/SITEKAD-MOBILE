package com.example.newtes

import android.content.Context

object ThemeManager {
    private const val PREFS_NAME = "SITEKAD_THEME_PREFS"
    private const val IS_DARK_MODE = "IS_DARK_MODE"

    fun saveTheme(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(IS_DARK_MODE, isDark).apply()
    }

    fun getTheme(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Default ke mode terang (false) jika belum ada settingan
        return prefs.getBoolean(IS_DARK_MODE, false)
    }
}