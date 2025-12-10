package com.example.bottomapp.shared.backend.settings

import com.example.bottomapp.shared.ui.screens.readerscreens.common.FlipMode
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

/**
 * Common settings manager interface for cross-platform settings
 */
interface ISettingsManager {
    /**
     * Get current app theme mode as a StateFlow
     */
    val appThemeModeFlow: StateFlow<AppThemeMode>

    /**
     * Get current app theme
     */
     fun getAppTheme(defaultTheme: AppThemeMode = AppThemeMode.System): AppThemeMode

    /**
     * Set app theme
     */
     fun setAppTheme(theme: AppThemeMode)

    /**
     * Get documents display theme
     */
     fun getDocumentsTheme(defaultTheme: DocThemeMode = DocThemeMode.AsIs): DocThemeMode

    /**
     * Set documents theme
     */
     fun setDocumentsTheme(theme: DocThemeMode)

    /**
     * Get PDF flip animation mode
     */
     fun getFlipMode(): FlipMode

    /**
     * Set PDF flip mode
     */
     fun setFlipMode(mode: FlipMode)

    /**
     * Get storage directory path
     */
     fun getStoragePath(): String?

    /**
     * Set storage directory path
     */
     fun setStoragePath(path: String)

    /**
     * Clear storage path
     */
     fun clearStoragePath()
}

/**
 * Default settings implementation
 */
class DefaultSettingsManager(
    private val store: com.example.bottomapp.shared.backend.storage.PreferencesStore
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
        return if (path.isEmpty()) null else path
    }

    override  fun setStoragePath(path: String) {
        store.putString("storage_path", path)
    }

    override  fun clearStoragePath() {
        store.removeKey("storage_path")
    }
}

