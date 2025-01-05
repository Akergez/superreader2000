package com.example.bottomapp.ui.readerscreens.pdfscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.renderHelper.PdfRendererHelper
import com.example.bottomapp.ui.readerscreens.common.ReaderMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfScreenViewModel(
    private val settingsManager: SettingsManager,
    private val pdfBookDomainModel: PdfBookDomainModel,
    val pdfRendererHelper: PdfRendererHelper
) : ViewModel() {
    private val _pdfScreenState = MutableStateFlow(PdfScreenState())
    val pdfScreenState: StateFlow<PdfScreenState> = _pdfScreenState.asStateFlow()

    init {
        val docThemeMode = settingsManager.getDocumentsTheme(DocThemeMode.AsIs)
        val pdfFlipMode = settingsManager.getFlipMode()
        _pdfScreenState.value = PdfScreenState(
            pdfBookDomainModel.readProgress().pageIndex,
            ReaderMode.Reading,
            pdfFlipMode,
            docThemeMode
        )
    }

    fun switchReaderMode(newMode: ReaderMode) {
        _pdfScreenState.update { it.copy(mode = newMode) }
    }

    fun changePage(newPage: Int) {
        _pdfScreenState.update {
            it.copy(
                currentPage = newPage,
            )
        }
        viewModelScope.launch { pdfBookDomainModel.saveProgress(PdfProgress(newPage)) }
    }

    fun updateDocumentThemeMode(docThemeMode: DocThemeMode) {
        settingsManager.setDocumentsTheme(docThemeMode)
        _pdfScreenState.update { it.copy(docThemeMode = docThemeMode) }
    }
}