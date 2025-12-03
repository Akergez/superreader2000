package com.example.bottomapp.shared.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reading mode for the PDF reader
 */
enum class ReaderMode {
    Reading,
    Bookmarks,
    Settings,
    TableOfContents
}

/**
 * Immutable state for PDF reader screen
 */
data class PdfReaderUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val mode: ReaderMode = ReaderMode.Reading,
    val flipMode: FlipMode = FlipMode.Vertical,
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
    val isLoading: Boolean = false
)

/**
 * Koin-managed state manager for PDF reader.
 * Factory-scoped (one instance per reader session).
 * Replaces Android ViewModel pattern.
 */
class PdfReaderState(
    private val pdfBookDomainModel: Any, // PdfBookDomainModel from shared backend
    private val settingsManager: Any // SettingsManager from shared backend
) {
    private val _uiState = MutableStateFlow(PdfReaderUiState())
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    private var currentPage: Int = 0
    private val stateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        initializeState()
    }

    private fun initializeState() {
        // TODO: Load from pdfBookDomainModel
        // val progress = pdfBookDomainModel.readProgress()
        // val docTheme = settingsManager.getDocumentsTheme(DocThemeMode.AsIs)
        // val flipMode = settingsManager.getFlipMode()
        // _uiState.value = PdfReaderUiState(
        //     currentPage = progress.pageIndex,
        //     totalPages = progress.totalPages,
        //     flipMode = flipMode,
        //     docThemeMode = docTheme
        // )
        // currentPage = progress.pageIndex
    }

    fun switchReaderMode(newMode: ReaderMode) {
        _uiState.update { it.copy(mode = newMode) }
    }

    fun getTitle(): String {
        // TODO: return pdfBookDomainModel.title
        return "PDF Reader"
    }

    fun changePage(newPage: Int) {
        currentPage = newPage
        _uiState.update { it.copy(currentPage = newPage) }

        stateScope.launch {
            // TODO: Save progress
            // pdfBookDomainModel.saveProgress(PdfProgress(newPage, totalPages))
        }
    }

    fun updateDocumentThemeMode(docThemeMode: DocThemeMode) {
        // TODO: settingsManager.setDocumentsTheme(docThemeMode)
        _uiState.update { it.copy(docThemeMode = docThemeMode) }
    }

    fun updateFlipMode(flipMode: FlipMode) {
        // TODO: settingsManager.setPdfFlipMode(flipMode)
        _uiState.update { it.copy(flipMode = flipMode) }
    }

    fun goToNextPage() {
        val currentState = _uiState.value
        if (currentState.currentPage < currentState.totalPages - 1) {
            changePage(currentState.currentPage + 1)
        }
    }

    fun goToPreviousPage() {
        val currentState = _uiState.value
        if (currentState.currentPage > 0) {
            changePage(currentState.currentPage - 1)
        }
    }

    fun goToPage(pageNumber: Int) {
        val currentState = _uiState.value
        if (pageNumber >= 0 && pageNumber < currentState.totalPages) {
            changePage(pageNumber)
        }
    }
}

