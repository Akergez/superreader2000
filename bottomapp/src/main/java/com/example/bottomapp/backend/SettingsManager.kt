package com.example.bottomapp.backend
import android.content.Context


enum class AppThemeMode(val value: String) {
    Light("Light"),
    Dark("Dark"),
    System("System")
}

enum class DocThemeMode(val value: String) {
    AsIs("AsIs"),
    Invert("Invert")
}


class SettingsManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val appThemeKey = "theme"
    private val docThemeKey = "doc_theme"

    private fun saveSetting(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun getSetting(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun getAppTheme(defaultTheme: AppThemeMode): AppThemeMode {
        return try {
            AppThemeMode.valueOf(getSetting(appThemeKey, defaultTheme.value))
        } catch (e: Throwable) {
            defaultTheme
        }
    }

    fun setAppTheme(theme: AppThemeMode) {
        saveSetting(appThemeKey, theme.value)
    }

    fun getDocumentsTheme(defaultTheme: DocThemeMode): DocThemeMode {
        return try {
            DocThemeMode.valueOf(getSetting(docThemeKey, defaultTheme.value))
        } catch (e: Throwable) {
            defaultTheme
        }
    }

    fun setDocumentsTheme(theme: DocThemeMode) {
        saveSetting(docThemeKey, theme.value)
    }

    fun getStorage(defaultTheme: String): String {
        return getSetting("storage", defaultTheme)
    }

    fun setStorage(theme: String) {
        saveSetting("storage", theme)
    }
}