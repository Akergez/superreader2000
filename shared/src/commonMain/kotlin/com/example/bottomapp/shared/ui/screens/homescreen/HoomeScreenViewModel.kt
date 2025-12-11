package com.example.bottomapp.shared.ui.screens.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.domain.FileType
import com.example.bottomapp.shared.backend.bookCreators.IBookCreateFactory
import com.example.bottomapp.shared.backend.storage.BookStorageEntry
import com.example.bottomapp.shared.platform.FileUriHandler
import com.example.bottomapp.shared.platform.MessageHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HoomeScreenViewModel(
    private val bookManager: BookManager,
    private val factories: Map<FileType, IBookCreateFactory>,
    private val messageHandler: MessageHandler,
    private val fileUriHandler: FileUriHandler
) : ViewModel() {

    private val _hoomeScreenState = MutableStateFlow(HoomeScreenState())
    val hoomeScreenState: StateFlow<HoomeScreenState> = _hoomeScreenState.asStateFlow()
    private val _books = MutableStateFlow(emptyList<BookStorageEntry>())
    val selectedBooks = MutableStateFlow(emptyList<BookStorageEntry>())
    val books = _books

    init {
        viewModelScope.launch {
            _hoomeScreenState.value = HoomeScreenState()
            bookManager.saveBookLinks()
            bookManager.bookFlow.collect {
                _books.value = it
            }
        }
    }

    fun dumpBooks() {
        viewModelScope.launch {
            bookManager.saveBookLinks()
        }
    }

    fun addBook(fileUri: String, fileType: FileType) {
        viewModelScope.launch {
            val fileName = fileUriHandler.getFileName(fileUri)
            val factory = factories[fileType]
            if (factory == null) {
                if (fileType != FileType.unknown) {
                    messageHandler.showLongMessage("Unsupported file type")
                    return@launch
                }

                throw NotImplementedError("IBookCreateFactory for $fileType file type is not found. Check your DI configuration")
            }

            bookManager.addNewBook(
                fileUri, factory.getNameFromFileName(fileName), factory.createProgress(), fileType
            )
        }
    }

    fun updateSelectedBook(book: BookStorageEntry) {
        if (book in selectedBooks.value) {
            selectedBooks.value = selectedBooks.value.filter { x -> x != book }
        } else {
            selectedBooks.value = selectedBooks.value
            selectedBooks.update { currentList ->
                currentList.toMutableList().apply {
                    add(book)
                }
            }
        }
    }

    fun clearSelectedBooks() {
        selectedBooks.value = emptyList()
    }

    fun updateEditingBook(book: BookStorageEntry?) {
        _hoomeScreenState.value = _hoomeScreenState.value.copy(editingBook = book)
    }

    fun clearTemporaryState() {
        _hoomeScreenState.value = _hoomeScreenState.value.copy(editingBook = null)
        clearSelectedBooks()
    }

    fun onSaveEditModal(
        oldBook: BookStorageEntry,
        newBook: BookStorageEntry,
        changed: Boolean,
        previewChanged: Boolean
    ) {
        if (changed) {
            handleBookUpdate(oldBook, newBook, previewChanged)
        }
        updateEditingBook(null)
    }

    private fun handleBookUpdate(
        oldBook: BookStorageEntry,
        newBook: BookStorageEntry,
        previewChanged: Boolean
    ) {
        var updatedBook = newBook
        if (previewChanged) {
            viewModelScope.launch {
                bookManager.updateBookPreview(oldBook.bundleName, newBook.previewUri!!).onSuccess {
                    updatedBook = newBook.copy(previewUri = it)
                }.onSuccess {
                    viewModelScope.launch {
                        bookManager.renameBook(
                            oldBook.bundleName, updatedBook.bundleName
                        ).onFailure {
                            messageHandler.showLongMessage(it.message ?: "Error renaming book")
                        }.onSuccess {
                            if (newBook.bookMetaInfo.author != oldBook.bookMetaInfo.author) {
                                viewModelScope.launch {
                                    bookManager.updateAuthor(
                                        newBook.bundleName, newBook.bookMetaInfo.author
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return
        }
        if (newBook.bundleName != oldBook.bundleName) {
            viewModelScope.launch {
                bookManager.renameBook(
                    oldBook.bundleName, updatedBook.bundleName
                ).onFailure {
                    messageHandler.showLongMessage(it.message ?: "Error renaming book")
                }.onSuccess {
                    if (newBook.bookMetaInfo.author != oldBook.bookMetaInfo.author) {
                        viewModelScope.launch {
                            bookManager.updateAuthor(
                                newBook.bundleName, newBook.bookMetaInfo.author
                            )
                        }
                    }
                }
            }
            return
        }

        if (newBook.bookMetaInfo.author != oldBook.bookMetaInfo.author) {
            viewModelScope.launch {
                bookManager.updateAuthor(newBook.bundleName, newBook.bookMetaInfo.author)
            }
        }
    }

    fun onDeleteBooks(
        books: List<BookStorageEntry>
    ) {
        viewModelScope.launch {
            books.map { book ->
                bookManager.deleteBook(book.bundleName).onFailure {
                    messageHandler.showLongMessage(it.message ?: "Error deleting book")
                }
            }
        }
        clearSelectedBooks()
    }
}

