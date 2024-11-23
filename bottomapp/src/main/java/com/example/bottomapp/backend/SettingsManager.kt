package com.example.bottomapp.backend

import android.content.Context
import android.net.Uri
import com.example.bottomapp.ui.readerscreens.common.FlipMode


enum class AppThemeMode {
    Light,
    Dark,
    System
}

enum class DocThemeMode {
    AsIs,
    Invert
}

enum class PdfFlipMode {
    Curl,
    Scroll,
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
        saveSetting(appThemeKey, theme.name)
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
        saveSetting(docThemeKey, theme.name)
    }

    fun getFlipMode(): FlipMode {
        val defaultMode = FlipMode.PagingCurl
        val setting = getSetting(pdfFlipModeKey) ?: return defaultMode
        return try {
            FlipMode.valueOf(setting)
        } catch (e: Throwable) {
            defaultMode
        }
    }

    fun setPdfFlipMode(mode: FlipMode) {
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