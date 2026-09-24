package com.danstudios.reelnotes.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var preferredLanguage: String
        get() = prefs.getString(KEY_PREFERRED_LANGUAGE, "fr") ?: "fr"
        set(value) = prefs.edit().putString(KEY_PREFERRED_LANGUAGE, value).apply()

    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_RUN, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_RUN, value).apply()

    companion object {
        private const val PREFS_NAME = "reelnotes_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_PREFERRED_LANGUAGE = "preferred_language"
        private const val KEY_IS_FIRST_RUN = "is_first_run"
    }
}
