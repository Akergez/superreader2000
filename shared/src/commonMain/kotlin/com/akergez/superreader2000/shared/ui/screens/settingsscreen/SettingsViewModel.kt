package com.akergez.superreader2000.shared.ui.screens.settingsscreen

import androidx.lifecycle.ViewModel
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.backend.settings.ReaderFont
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SettingsViewModel(
    private val settingsManager: ISettingsManager,
    private val bookManager: BookManager,
    private val json: Json
) : ViewModel() {
    private val _settingsState = MutableStateFlow(SettingsScreenState())
    val settingsState: StateFlow<SettingsScreenState> = _settingsState.asStateFlow()
    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    init {
        val textDocThemeMode = settingsManager.getTextDocumentsTheme(DocThemeMode.AsIs)
        val fixedDocThemeMode = settingsManager.getFixedDocumentsTheme(DocThemeMode.AsIs)
        val pdfFlipMode = settingsManager.getFlipMode()
        val storageAddress = extractBeautifulStoragePath(settingsManager.getStoragePath())
        val appThemeMode = settingsManager.getAppTheme(AppThemeMode.System)
        _settingsState.value =
            SettingsScreenState(
                storageAddress = storageAddress,
                textDocThemeMode = textDocThemeMode,
                fixedDocThemeMode = fixedDocThemeMode,
                flipMode = pdfFlipMode,
                appThemeMode = appThemeMode,
                readerFont = settingsManager.getReaderFont(),
                readerFontSize = settingsManager.getReaderFontSize(),
                compatibilityTextRenderer = settingsManager.getCompatibilityTextRenderer()
            )
    }

    fun updateTextDocThemeMode(docThemeMode: DocThemeMode) {
        settingsManager.setTextDocumentsTheme(docThemeMode)
        _settingsState.value = _settingsState.value.copy(textDocThemeMode = docThemeMode)
    }

    fun updateFixedDocThemeMode(docThemeMode: DocThemeMode) {
        settingsManager.setFixedDocumentsTheme(docThemeMode)
        _settingsState.value = _settingsState.value.copy(fixedDocThemeMode = docThemeMode)
    }

    fun updateDocThemeMode(docThemeMode: DocThemeMode) = updateFixedDocThemeMode(docThemeMode)

    fun updateFlipMode(pdfFlipMode: FlipMode) {
        settingsManager.setFlipMode(pdfFlipMode)
        _settingsState.value = _settingsState.value.copy(flipMode = pdfFlipMode)
    }

    fun updateReaderFont(font: ReaderFont) {
        settingsManager.setReaderFont(font)
        _settingsState.value = _settingsState.value.copy(readerFont = font)
    }

    fun updateReaderFontSize(size: Float) {
        settingsManager.setReaderFontSize(size)
        _settingsState.value = _settingsState.value.copy(readerFontSize = size)
    }

    fun updateCompatibilityTextRenderer(enabled: Boolean) {
        settingsManager.setCompatibilityTextRenderer(enabled)
        _settingsState.value = _settingsState.value.copy(compatibilityTextRenderer = enabled)
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
