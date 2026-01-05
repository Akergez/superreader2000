package com.akergez.superreader2000.shared.ui.screens.settingsscreen

import androidx.lifecycle.ViewModel
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsManager: ISettingsManager,
    private val bookManager: BookManager
) : ViewModel() {
    private val _settingsState = MutableStateFlow(SettingsScreenState())
    val settingsState: StateFlow<SettingsScreenState> = _settingsState.asStateFlow()

    init {
        val docThemeMode = settingsManager.getDocumentsTheme(DocThemeMode.AsIs)
        val pdfFlipMode = settingsManager.getFlipMode()
        val storageAddress = extractBeautifulStoragePath(settingsManager.getStoragePath())
        val appThemeMode = settingsManager.getAppTheme(AppThemeMode.System)
        _settingsState.value =
            SettingsScreenState(storageAddress, docThemeMode, pdfFlipMode, appThemeMode)
    }

    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    fun updateDocThemeMode(docThemeMode: DocThemeMode) {
        settingsManager.setDocumentsTheme(docThemeMode)
        _settingsState.value = _settingsState.value.copy(docThemeMode = docThemeMode)
    }

    fun updateFlipMode(pdfFlipMode: FlipMode) {
        settingsManager.setFlipMode(pdfFlipMode)
        _settingsState.value = _settingsState.value.copy(flipMode = pdfFlipMode)
    }

    fun updateStorageAddress(storageAddress: String) {
        settingsManager.setStoragePath(storageAddress)
        uiScope.launch {
            bookManager.refreshBooksLinks()
        }
        _settingsState.value = _settingsState.value.copy(storageAddress = extractBeautifulStoragePath(settingsManager.getStoragePath()))
    }

    fun updateAppThemeMode(appThemeMode: AppThemeMode) {
        settingsManager.setAppTheme(appThemeMode)
        _settingsState.value = _settingsState.value.copy(appThemeMode = appThemeMode)
    }

    private fun extractBeautifulStoragePath(path: String?) : String {
        return path ?: ""
    }
}