package com.badalbiswas.filevault

import android.content.Context

object AppPreferences {
    private const val PREF_NAME = "app_settings"
    private const val KEY_SHOW_HIDDEN = "show_hidden_files"

    fun getShowHidden(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_HIDDEN, false)
    }

    fun setShowHidden(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_HIDDEN, value).apply()
    }
}
