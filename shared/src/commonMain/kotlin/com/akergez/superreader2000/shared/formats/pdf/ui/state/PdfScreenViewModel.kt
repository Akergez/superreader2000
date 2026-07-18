package com.akergez.superreader2000.shared.formats.pdf.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.formats.pdf.PdfBookDomainModel
import com.akergez.superreader2000.shared.formats.pdf.PdfProgress
import com.akergez.superreader2000.shared.formats.pdf.PdfRendererHelper
import com.akergez.superreader2000.shared.formats.pdf.ui.components.PdfScreenState
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.ReaderMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfScreenViewModel(
    private val settingsManager: ISettingsManager,
    private val pdfBookDomainModel: PdfBookDomainModel,
    val pdfRendererHelper: PdfRendererHelper
) : ViewModel() {
    private val _pdfScreenState = MutableStateFlow(PdfScreenState())
    val pdfScreenState: StateFlow<PdfScreenState> = _pdfScreenState.asStateFlow()
    var currentPage: Int = 0

    init {
        val docThemeMode = settingsManager.getFixedDocumentsTheme(DocThemeMode.AsIs)
        val pdfFlipMode = settingsManager.getFlipMode()
        _pdfScreenState.value = PdfScreenState(
            pdfBookDomainModel.readProgress().pageIndex,
            ReaderMode.Reading,
            pdfFlipMode,
            docThemeMode
        )
        currentPage = pdfBookDomainModel.readProgress().pageIndex
    }

    fun switchReaderMode(newMode: ReaderMode) {
        _pdfScreenState.update { it.copy(mode = newMode) }
    }

    fun getTitle():String {
        return pdfBookDomainModel.getMetadata().title
    }

    fun changePage(newPage: Int) {
        _pdfScreenState.update {
            it.copy(
                currentPage = newPage,
            )
        }
        currentPage = newPage
        viewModelScope.launch { pdfBookDomainModel.saveProgress(
            PdfProgress(
                newPage,
                pdfRendererHelper.pagesCount.value ?: 0
            )
        ) }
    }

    fun updateDocumentThemeMode(docThemeMode: DocThemeMode) {
        settingsManager.setFixedDocumentsTheme(docThemeMode)
        _pdfScreenState.update { it.copy(docThemeMode = docThemeMode) }
    }

    fun updateFlipMode(flipMode: FlipMode) {
        settingsManager.setFlipMode(flipMode)
        _pdfScreenState.value = _pdfScreenState.value.copy(curlMode = flipMode)
    }
}
