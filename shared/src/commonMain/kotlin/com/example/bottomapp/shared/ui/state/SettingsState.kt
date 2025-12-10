package com.example.bottomapp.shared.ui.state

import com.example.bottomapp.shared.backend.settings.AppThemeMode
import com.example.bottomapp.shared.backend.settings.DocThemeMode
import com.example.bottomapp.shared.ui.screens.readerscreens.common.FlipMode
import com.sun.jndi.toolkit.url.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.file.Path

/**
 * Immutable state for settings screen
 */
data class SettingsUiState(
    val storageAddress: String = "",
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
    val flipMode: FlipMode = FlipMode.VerticalScroll,
    val appThemeMode: AppThemeMode = AppThemeMode.System
)

/**
 * Koin-managed state manager for settings.
 * Replaces Android ViewModel pattern.
 */
class SettingsState(
    private val settingsManager: Any, // SettingsManager from shared backend
    private val bookManager: Any // BookManager for refresh
) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val stateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        initializeState()
    }

    private fun initializeState() {
        // TODO: Load settings from settingsManager
        // val docTheme = settingsManager.getDocumentsTheme(DocThemeMode.AsIs)
        // val flipMode = settingsManager.getFlipMode()
        // val storagePath = extractBeautifulStoragePath(settingsManager.getStorageUri())
        // val appTheme = settingsManager.getAppTheme(AppThemeMode.System)
        // _uiState.value = SettingsUiState(
        //     storageAddress = storagePath,
        //     docThemeMode = docTheme,
        //     flipMode = flipMode,
        //     appThemeMode = appTheme
        // )
    }

    fun updateDocThemeMode(docThemeMode: DocThemeMode) {
        // TODO: settingsManager.setDocumentsTheme(docThemeMode)
        _uiState.update { it.copy(docThemeMode = docThemeMode) }
    }

    fun updateFlipMode(flipMode: FlipMode) {
        // TODO: settingsManager.setPdfFlipMode(flipMode)
        _uiState.update { it.copy(flipMode = flipMode) }
    }

    fun updateStorageAddress(storageAddress: String) {
        // TODO: settingsManager.saveStorageUri(storageAddress)
        // TODO: bookManager.refreshBooksLinks()
        stateScope.launch {
            _uiState.update {
                it.copy(storageAddress = extractBeautifulStoragePath(storageAddress))
            }
        }
    }

    fun updateAppThemeMode(appThemeMode: AppThemeMode) {
        // TODO: settingsManager.setAppTheme(appThemeMode)
        _uiState.update { it.copy(appThemeMode = appThemeMode) }
    }

    private fun extractBeautifulStoragePath(path: String?): String {
        // return path?.lastPathSegment.toString()
        return "path"
    }
}

