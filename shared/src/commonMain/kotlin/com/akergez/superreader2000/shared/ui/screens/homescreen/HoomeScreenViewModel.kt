package com.akergez.superreader2000.shared.ui.screens.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.formats.abstractions.IBookCreateFactory
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry
import com.akergez.superreader2000.shared.formats.abstractions.FileType
import com.akergez.superreader2000.shared.platform.FileUriHandler
import com.akergez.superreader2000.shared.platform.MessageHandler
import com.akergez.tresse.kmp.TresseConfig
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
    private var syncConfig: TresseConfig? = null

    init {
        viewModelScope.launch {
            _hoomeScreenState.value = HoomeScreenState()
            bookManager.saveBookLinks()
            refreshSyncAvailability()
            bookManager.bookFlow.collect {
                _books.value = it
            }
        }
    }

    fun dumpBooks() {
        viewModelScope.launch {
            bookManager.saveBookLinks()
            refreshSyncAvailability()
        }
    }

    fun synchronize() {
        if (_hoomeScreenState.value.isSyncRunning) return

        viewModelScope.launch {
            val config = syncConfig ?: loadConfiguredSync()
            if (config == null) return@launch

            _hoomeScreenState.update { it.copy(isSyncRunning = true) }
            bookManager.synchronize(config).onFailure { error ->
                messageHandler.showLongMessage(error.message ?: "Synchronization failed")
            }
            _hoomeScreenState.update { it.copy(isSyncRunning = false) }
        }
    }

    private suspend fun refreshSyncAvailability() {
        syncConfig = loadConfiguredSync()
        _hoomeScreenState.update {
            it.copy(isSyncConfigured = syncConfig != null)
        }
    }

    private suspend fun loadConfiguredSync(): TresseConfig? =
        runCatching { bookManager.loadS3Config() }.getOrNull()?.takeIf(::isValidS3Config)

    private fun isValidS3Config(config: TresseConfig): Boolean =
        config.s3Endpoint.isNotBlank() &&
            config.s3Bucket.isNotBlank() &&
            config.s3AccessKeyId.isNotBlank() &&
            config.s3SecretAccessKey.isNotBlank() &&
            config.encryptionKey.isNotBlank()

    fun addBook(fileUri: String, fileType: FileType? = null) {
        viewModelScope.launch {
            val fileName = fileUriHandler.getFileName(fileUri)
            val detectedType = fileType ?: fileUriHandler.getFileType(fileUri)
            if (detectedType == FileType.unknown) {
                messageHandler.showLongMessage("Unsupported file type")
                return@launch
            }
            val factory = factories[detectedType]
            if (factory == null) {
                messageHandler.showLongMessage("Unsupported file type")
                return@launch
            }

            bookManager.addNewBook(
                fileUri, factory.getNameFromFileName(fileName), factory.createProgress(), detectedType
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
