package com.example.bottomapp.shared.ui.state

import com.example.bottomapp.shared.backend.storage.BookStorageEntry
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
 * Immutable state for book editing modal
 */
data class EditBookUiState(
    val book: BookStorageEntry,
    val editedBook: BookStorageEntry = book,
    val previewUri: String? = null,
    val hasChanged: Boolean = false,
    val previewChanged: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * Koin-managed state manager for book editing modal.
 * Factory-scoped with book parameter.
 * Replaces Android ViewModel pattern.
 */
class EditBookState(
    initialBook: BookStorageEntry,
    private val bookManager: Any, // BookManager from shared backend
    private val platformServices: IPlatformServices
) {
    private val _uiState = MutableStateFlow(EditBookUiState(initialBook))
    val uiState: StateFlow<EditBookUiState> = _uiState.asStateFlow()

    private val stateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun updateBookTitle(newTitle: String) {
        _uiState.update { state ->
            val updated = state.editedBook.copy(
                bundleName = newTitle,
                bookMetaInfo = state.editedBook.bookMetaInfo.copy(title = newTitle)
            )
            state.copy(
                editedBook = updated,
                hasChanged = updated != state.book
            )
        }
    }

    fun updateBookAuthor(newAuthor: String) {
        _uiState.update { state ->
            val updated = state.editedBook.copy(
                bookMetaInfo = state.editedBook.bookMetaInfo.copy(author = newAuthor)
            )
            state.copy(
                editedBook = updated,
                hasChanged = updated != state.book
            )
        }
    }

    fun updateBookPreview(newPreviewUri: String) {
        _uiState.update { state ->
            val updated = state.editedBook.copy(previewUri = newPreviewUri)
            state.copy(
                editedBook = updated,
                previewUri = newPreviewUri,
                previewChanged = true,
                hasChanged = true
            )
        }
    }

    fun saveChanges(callback: (success: Boolean) -> Unit) {
        stateScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val currentState = _uiState.value
                val oldBook = currentState.book
                val newBook = currentState.editedBook

                // TODO: Call bookManager to handle updates
                // val success = bookManager.updateBook(oldBook, newBook)

                _uiState.update { it.copy(isSaving = false) }
                callback(true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Unknown error"
                    )
                }
                platformServices.showError(e.message ?: "Unknown error")
                callback(false)
            }
        }
    }

    fun resetChanges() {
        _uiState.update {
            it.copy(
                editedBook = it.book,
                hasChanged = false,
                previewChanged = false,
                error = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

