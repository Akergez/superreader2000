package com.example.bottomapp.backend

import android.content.Context
import android.net.Uri
import com.example.bottomapp.ui.readerscreens.common.CurlMode


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
    private val pdfFlipModeKey = "pdf_flip_mode"

    private fun saveSetting(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun getSetting(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun getAppTheme(defaultTheme: AppThemeMode): AppThemeMode {
        val setting = getSetting(appThemeKey) ?: return defaultTheme
        return try {
            AppThemeMode.valueOf(setting)
        } catch (e: Throwable) {
            defaultTheme
        }
    }

    fun setAppTheme(theme: AppThemeMode) {
        saveSetting(appThemeKey, theme.value)
    }

    fun getDocumentsTheme(defaultTheme: DocThemeMode): DocThemeMode {
        val setting = getSetting(docThemeKey) ?: return defaultTheme
        return try {
            DocThemeMode.valueOf(setting)
        } catch (e: Throwable) {
            defaultTheme
        }
    }

    fun setDocumentsTheme(theme: DocThemeMode) {
        saveSetting(docThemeKey, theme.value)
    }

    fun getCurlMode(): CurlMode {
        val defaultMode = CurlMode.PagingCurl
        val setting = getSetting(pdfFlipModeKey) ?: return defaultMode
        return try {
            CurlMode.valueOf(setting)
        } catch (e: Throwable) {
            defaultMode
        }
    }

    fun setCurlMode(mode: CurlMode) {
        saveSetting(pdfFlipModeKey, mode.toString())
    }

    fun getStorageUri(): Uri? {
        val setting: String = getSetting("storage") ?: return null
        return Uri.parse(setting)
    }

    fun saveStorageUri(uri: Uri) {
        saveSetting("storage", uri.toString())
    }
}