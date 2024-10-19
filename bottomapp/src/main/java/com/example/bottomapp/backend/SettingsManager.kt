package com.example.bottomapp.backend
import android.content.Context


class SettingsManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun saveSetting(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun getSetting(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun getAppTheme(defaultTheme: String): String {
        return getSetting("theme", defaultTheme)
    }

    fun setAppTheme(theme: String) {
        saveSetting("theme", theme)
    }

    fun getDocumentsTheme(defaultTheme: String): String {
        return getSetting("doc_theme", defaultTheme)
    }

    fun setDocumentsTheme(theme: String) {
        saveSetting("doc_theme", theme)
    }

    fun getStorage(defaultTheme: String): String {
        return getSetting("storage", defaultTheme)
    }

    fun setStorage(theme: String) {
        saveSetting("storage", theme)
    }
}