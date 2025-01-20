package com.example.bottomapp.ui.readerscreens.djvuscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bottomapp.backend.DjvuBookDomainModel
import com.example.bottomapp.backend.DjvuProgress
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.renderHelper.DJVURenderHelper
import com.example.bottomapp.ui.readerscreens.common.FlipMode
import com.example.bottomapp.ui.readerscreens.common.ReaderMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DjvuScreenViewModel(
    private val settingsManager: SettingsManager,
    private val djvuBookDomainModel: DjvuBookDomainModel,
    val djvuRenderHelper: DJVURenderHelper
) : ViewModel() {
    private val _djvuScreenState = MutableStateFlow(DjvuScreenState())
    val djvuScreenStateStateFlow: StateFlow<DjvuScreenState> = _djvuScreenState.asStateFlow()
    var currentPage: Int = 0

    init {
        val docThemeMode = settingsManager.getDocumentsTheme(DocThemeMode.AsIs)
        val djvuFlipMode = settingsManager.getFlipMode()
        _djvuScreenState.value = DjvuScreenState(
            djvuBookDomainModel.readProgress().pageIndex,
            ReaderMode.Reading,
            djvuFlipMode,
            docThemeMode
        )
        currentPage = djvuBookDomainModel.readProgress().pageIndex
    }

    fun switchReaderMode(newMode: ReaderMode) {
        _djvuScreenState.update { it.copy(mode = newMode) }
    }

    fun getTitle(): String {
        return djvuBookDomainModel.title
    }

    fun changePage(newPage: Int) {
        currentPage = newPage
        viewModelScope.launch {
            djvuBookDomainModel.saveProgress(
                DjvuProgress(
                    newPage,
                    djvuRenderHelper.pagesCount.value
                )
            )
        }
    }

    fun updateDocumentThemeMode(docThemeMode: DocThemeMode) {
        settingsManager.setDocumentsTheme(docThemeMode)
        _djvuScreenState.update { it.copy(docThemeMode = docThemeMode) }
    }

    fun updateFlipMode(flipMode: FlipMode) {
        settingsManager.setPdfFlipMode(flipMode)
        _djvuScreenState.value = _djvuScreenState.value.copy(curlMode = flipMode)
    }
}