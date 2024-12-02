package com.example.bottomapp.ui.settingsscreen

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.FlipMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val bookStorage: BookStorage
) : ViewModel() {
    private val _settingsState = MutableStateFlow(SettingsScreenState())
    val settingsState: StateFlow<SettingsScreenState> = _settingsState.asStateFlow()

    init {
        val docThemeMode = settingsManager.getDocumentsTheme(DocThemeMode.AsIs)
        val pdfFlipMode = settingsManager.getFlipMode()
        val storageAddress = settingsManager.getStorageUri()
        _settingsState.value =
            SettingsScreenState(storageAddress.toString(), docThemeMode, pdfFlipMode)
    }

    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    fun updateDocThemeMode(docThemeMode: DocThemeMode) {
        settingsManager.setDocumentsTheme(docThemeMode)
        _settingsState.value = _settingsState.value.copy(docThemeMode = docThemeMode)
    }

    fun updateFlipMode(pdfFlipMode: FlipMode) {
        settingsManager.setPdfFlipMode(pdfFlipMode)
        _settingsState.value = _settingsState.value.copy(flipMode = pdfFlipMode)
    }

    fun updateStorageAddress(storageAddress: Uri) {
        settingsManager.saveStorageUri(storageAddress)
        uiScope.launch {
            bookStorage.refreshBooksLinks()
        }
        _settingsState.value = _settingsState.value.copy(storageAddress = storageAddress.toString())
    }

    fun updateAppThemeMode(appThemeMode: AppThemeMode) {
        uiScope.launch {
            settingsManager.setAppTheme(appThemeMode)
        }
        _settingsState.value = _settingsState.value.copy(appThemeMode = appThemeMode)
    }
}