package com.example.bottomapp.shared.backend.settings

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
@Serializable
enum class DocThemeMode {
    AsIs,
    Invert
}

/**
 * PDF flip animation mode
 */
@Serializable
enum class FlipMode {
    PagingCurl,
    SlideAnimation,
    DirectFlip
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
    suspend fun getAppTheme(defaultTheme: AppThemeMode = AppThemeMode.System): AppThemeMode

    /**
     * Set app theme
     */
    suspend fun setAppTheme(theme: AppThemeMode)

    /**
     * Get documents display theme
     */
    suspend fun getDocumentsTheme(defaultTheme: DocThemeMode = DocThemeMode.AsIs): DocThemeMode

    /**
     * Set documents theme
     */
    suspend fun setDocumentsTheme(theme: DocThemeMode)

    /**
     * Get PDF flip animation mode
     */
    suspend fun getFlipMode(): FlipMode

    /**
     * Set PDF flip mode
     */
    suspend fun setFlipMode(mode: FlipMode)

    /**
     * Get storage directory path
     */
    suspend fun getStoragePath(): String?

    /**
     * Set storage directory path
     */
    suspend fun setStoragePath(path: String)

    /**
     * Clear storage path
     */
    suspend fun clearStoragePath()
}

/**
 * Default settings implementation
 */
class DefaultSettingsManager(
    private val store: com.example.bottomapp.shared.backend.storage.ISettingsStore
) : ISettingsManager {
    private val _appThemeModeFlow = kotlinx.coroutines.flow.MutableStateFlow(AppThemeMode.System)
    override val appThemeModeFlow: StateFlow<AppThemeMode> = _appThemeModeFlow

    override suspend fun getAppTheme(defaultTheme: AppThemeMode): AppThemeMode {
        val themeName = store.getString("app_theme", defaultTheme.name)
        return try {
            AppThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            defaultTheme
        }
    }

    override suspend fun setAppTheme(theme: AppThemeMode) {
        store.putString("app_theme", theme.name)
        _appThemeModeFlow.value = theme
    }

    override suspend fun getDocumentsTheme(defaultTheme: DocThemeMode): DocThemeMode {
        val themeName = store.getString("doc_theme", defaultTheme.name)
        return try {
            DocThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            defaultTheme
        }
    }

    override suspend fun setDocumentsTheme(theme: DocThemeMode) {
        store.putString("doc_theme", theme.name)
    }

    override suspend fun getFlipMode(): FlipMode {
        val modeName = store.getString("pdf_flip_mode", FlipMode.PagingCurl.name)
        return try {
            FlipMode.valueOf(modeName)
        } catch (e: Exception) {
            FlipMode.PagingCurl
        }
    }

    override suspend fun setFlipMode(mode: FlipMode) {
        store.putString("pdf_flip_mode", mode.name)
    }

    override suspend fun getStoragePath(): String? {
        val path = store.getString("storage_path", "")
        return if (path.isEmpty()) null else path
    }

    override suspend fun setStoragePath(path: String) {
        store.putString("storage_path", path)
    }

    override suspend fun clearStoragePath() {
        store.removeKey("storage_path")
    }
}

