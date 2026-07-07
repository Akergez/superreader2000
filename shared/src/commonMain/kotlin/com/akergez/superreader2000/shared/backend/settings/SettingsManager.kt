package com.akergez.superreader2000.shared.backend.settings

import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Application theme modes
 */
@Serializable
enum class AppThemeMode {
    Light,
    Dark,
    System
}

/**
 * Document display theme modes
 */
enum class DocThemeMode {
    AsIs,
    Invert
}

interface ISettingsManager {
    val appThemeModeFlow: StateFlow<AppThemeMode>
     fun getAppTheme(defaultTheme: AppThemeMode = AppThemeMode.System): AppThemeMode
     fun setAppTheme(theme: AppThemeMode)
     fun getDocumentsTheme(defaultTheme: DocThemeMode = DocThemeMode.AsIs): DocThemeMode
     fun setDocumentsTheme(theme: DocThemeMode)
     fun getFlipMode(): FlipMode
     fun setFlipMode(mode: FlipMode)
     fun getStoragePath(): String?
     fun setStoragePath(path: String)
     fun clearStoragePath()
}

class DefaultSettingsManager(
    private val store: com.akergez.superreader2000.shared.backend.storage.PreferencesStore
) : ISettingsManager {
    private val _appThemeModeFlow = kotlinx.coroutines.flow.MutableStateFlow(AppThemeMode.System)
    override val appThemeModeFlow: StateFlow<AppThemeMode> = _appThemeModeFlow

    override  fun getAppTheme(defaultTheme: AppThemeMode): AppThemeMode {
        val themeName = store.getString("app_theme", defaultTheme.name)
        return try {
            AppThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            defaultTheme
        }
    }

    override  fun setAppTheme(theme: AppThemeMode) {
        store.putString("app_theme", theme.name)
        _appThemeModeFlow.value = theme
    }

    override  fun getDocumentsTheme(defaultTheme: DocThemeMode): DocThemeMode {
        val themeName = store.getString("doc_theme", defaultTheme.name)
        return try {
            DocThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            defaultTheme
        }
    }

    override  fun setDocumentsTheme(theme: DocThemeMode) {
        store.putString("doc_theme", theme.name)
    }

    override  fun getFlipMode(): FlipMode {
        val modeName = store.getString("pdf_flip_mode", FlipMode.PagingCurl.name)
        return try {
            FlipMode.valueOf(modeName)
        } catch (e: Exception) {
            FlipMode.PagingCurl
        }
    }

    override  fun setFlipMode(mode: FlipMode) {
        store.putString("pdf_flip_mode", mode.name)
    }

    override  fun getStoragePath(): String? {
        val path = store.getString("storage_path", "")
        return path.ifEmpty { null }
    }

    override  fun setStoragePath(path: String) {
        store.putString("storage_path", path)
    }

    override  fun clearStoragePath() {
        store.removeKey("storage_path")
    }

}
