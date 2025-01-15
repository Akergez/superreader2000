package com.example.bottomapp.backend

import android.content.Context
import android.net.Uri
import com.example.bottomapp.ui.readerscreens.common.FlipMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


enum class AppThemeMode {
    Light,
    Dark,
    System
}

enum class DocThemeMode {
    AsIs,
    Invert
}

class SettingsManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val appThemeKey = "theme"
    private val docThemeKey = "doc_theme"
    private val pdfFlipModeKey = "pdf_flip_mode"
    private val _appThemeMode = MutableStateFlow(getAppTheme(AppThemeMode.System))
    val appThemeMode: StateFlow<AppThemeMode> get() = _appThemeMode


    private fun saveSetting(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun getSetting(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun getAppTheme(defaultTheme: AppThemeMode): AppThemeMode {
        val setting = getSetting(appThemeKey) ?: return defaultTheme
        return AppThemeMode.valueOf(setting)
    }

    fun setAppTheme(theme: AppThemeMode) {
        saveSetting(appThemeKey, theme.name)
        _appThemeMode.value = theme
    }

    fun getDocumentsTheme(defaultTheme: DocThemeMode): DocThemeMode {
        val setting = getSetting(docThemeKey) ?: return defaultTheme
        return DocThemeMode.valueOf(setting)
    }

    fun setDocumentsTheme(theme: DocThemeMode) {
        saveSetting(docThemeKey, theme.name)
    }

    fun getFlipMode(): FlipMode {
        val defaultMode = FlipMode.PagingCurl
        val setting = getSetting(pdfFlipModeKey) ?: return defaultMode
        return FlipMode.valueOf(setting)
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