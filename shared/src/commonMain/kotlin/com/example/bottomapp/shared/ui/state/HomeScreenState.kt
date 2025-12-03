package com.example.bottomapp.shared.ui.state

import com.example.bottomapp.shared.backend.storage.BookStorageEntry
import com.example.bottomapp.shared.ui.platform.IFileService
import com.example.bottomapp.shared.ui.platform.IPlatformServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable state for the home/library screen.
 */
data class HomeScreenUiState(
    val editingBook: BookStorageEntry? = null,
    val selectedBooks: List<BookStorageEntry> = emptyList(),
    val books: List<BookStorageEntry> = emptyList(),
)


/**
 * Koin-managed state manager for the Home screen.
 * Replaces Android ViewModel pattern.
 */
class HomeScreenState(
    private val bookManager: Any, // BookManager from shared backend
    private val factories: Map<String, Any>, // Format type -> Factory map
    private val platformServices: IPlatformServices,
    private val fileService: IFileService
) {
    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    // Coroutine scope for this state manager (not tied to Android ViewModel lifecycle)
    private val stateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        initializeState()
    }

    private fun initializeState() {
        stateScope.launch {
            // TODO: Initialize from bookManager
            // bookManager.bookFlow.collect { books ->
            //     _uiState.update { it.copy(books = books) }
            // }
        }
    }

    fun updateSelectedBook(book: BookStorageEntry) {
        val currentSelected = _uiState.value.selectedBooks
        val newSelected = if (book in currentSelected) {
            currentSelected.filter { it != book }
        } else {
            currentSelected + book
        }
        _uiState.update { it.copy(selectedBooks = newSelected) }
    }

    fun clearSelectedBooks() {
        _uiState.update { it.copy(selectedBooks = emptyList()) }
    }

    fun updateEditingBook(book: BookStorageEntry?) {
        _uiState.update { it.copy(editingBook = book) }
    }

    fun clearTemporaryState() {
        _uiState.update { it.copy(editingBook = null, selectedBooks = emptyList()) }
    }

    fun onSaveEditModal(
        oldBook: BookStorageEntry,
        newBook: BookStorageEntry,
        changed: Boolean,
        previewChanged: Boolean
    ) {
        if (changed) {
            // TODO: Handle book update through bookManager
        }
        updateEditingBook(null)
    }

    fun onDeleteBooks(books: List<BookStorageEntry>) {
        stateScope.launch {
            for (book in books) {
                // TODO: Delete through bookManager
                // bookManager.deleteBook(book.bundleName).onFailure { error ->
                //     platformServices.showError(error.message ?: "Unknown error")
                // }
            }
            clearSelectedBooks()
        }
    }

    fun dumpBooks() {
        stateScope.launch {
            // TODO: Save book links through bookManager
            // bookManager.saveBookLinks()
        }
    }
}

