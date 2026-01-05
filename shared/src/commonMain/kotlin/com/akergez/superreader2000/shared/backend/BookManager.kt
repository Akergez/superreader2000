package com.akergez.superreader2000.shared.backend

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.backend.storage.BookRepository
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry
import com.akergez.superreader2000.shared.constants.toFileExtension
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.FileType
import com.akergez.superreader2000.shared.formats.abstractions.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookManager(
    private val storageManager: BookStorage,
    private val bookCache: BookRepository,
    private val messageHandler: com.akergez.superreader2000.shared.platform.MessageHandler
) {
    private val _bookLinks = MutableStateFlow<List<BookStorageEntry>>(emptyList())
    val bookFlow = _bookLinks.asStateFlow()
    private val _pendingBooksToSave = HashSet<String>(emptyList())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            bookCache.bookFlow.collect {
                _bookLinks.value = it
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            refreshBooksLinks()
        }
    }

    suspend fun getBookFile(bundleName: String): Result<FileWrapper> {
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))
        return storageManager.getBookFile(bundleName, meta.bookMetaInfo)
    }

    suspend fun addNewBook(
        fileUri: String,
        fileName: String,
        progress: Progress,
        fileType: FileType
    ): Result<Unit> {
        storageManager.createBundle(
            fileName,
            fileUri,
            BookMetaInfo(fileName, progress),
            fileType.toFileExtension()
        ).onSuccess {
            CoroutineScope(Dispatchers.IO).launch {
                bookCache.addBook(
                    BookStorageEntry(
                        fileName,
                        it.previewUri,
                        fileName,
                        it.meta
                    )
                )
            }
            return Result.success(Unit)
        }.onFailure {
            messageHandler.showMessage(it.message ?: "Failed to add book")
            return Result.failure(it)
        }
        return Result.success(Unit)
    }

    suspend fun refreshBooksLinks() {
        val list = getBooksLinksFromStorage()
        cacheBooksLinks(list)
    }

    suspend fun updateProgress(bundleName: String, bookMetaInfo: BookMetaInfo) {
        val book = bookCache.findBook(bookMetaInfo.title)
        bookCache.updateBook(
            BookStorageEntry(
                bookMetaInfo.title,
                book?.previewUri,
                bundleName,
                bookMetaInfo
            )
        )
        _pendingBooksToSave.add(bundleName)
    }

    suspend fun deleteBook(bundleName: String): Result<Unit> {
        storageManager.deleteBundle(bundleName)
            .onSuccess {
                refreshBooksLinks()
                return Result.success(Unit)
            }.onFailure {
                return Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    suspend fun renameBook(bundleName: String, newNameForBundle: String): Result<Unit> {
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))

        storageManager.renameBundle(bundleName, newNameForBundle, meta.bookMetaInfo)
            .onSuccess {
                storageManager.updateBundleMeta(
                    newNameForBundle,
                    BookMetaInfo(newNameForBundle, meta.bookMetaInfo.progress)
                ).onSuccess {
                    refreshBooksLinks()
                    return Result.success(Unit)
                }.onFailure {
                    storageManager.renameBundle(newNameForBundle, bundleName, meta.bookMetaInfo)
                    return Result.failure<Unit>(it)
                }
            }
            .onFailure {
                return Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    suspend fun updateAuthor(bundleName: String, author: String?): Result<Unit> {
        println("BookManager - updateAuthor: $author")
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))

        storageManager.updateBundleMeta(bundleName, meta.bookMetaInfo.copy(author = author))
            .onSuccess {
                refreshBooksLinks()
                return Result.success(Unit)
            }
            .onFailure {
                return Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    suspend fun updateBookPreview(
        bundleName: String,
        newPreviewUri: String
    ): Result<String> {
        val newPreview = storageManager.updateBundlePreview(newPreviewUri, bundleName)
        newPreview.onSuccess {
            refreshBooksLinks()
            return Result.success(it)
        }.onFailure {
            return Result.failure<String>(it.cause ?: it)
        }
        return Result.failure(Exception("Failed to update preview"))
    }

    suspend fun saveBookLinks() {
        println("BookManager - saveBookLinks")
        bookFlow.value.map {
            if (_pendingBooksToSave.contains(it.bundleName)) {
                _pendingBooksToSave.remove(it.bundleName)
                storageManager.updateBundleMeta(it.bundleName, it.bookMetaInfo)
            }
        }
    }

    private suspend fun getBooksLinksFromStorage(): List<BookStorageEntry> {
        return storageManager.listAllBundles().filterNotNull().map {
            BookStorageEntry(
                it.meta.title,
                it.previewUri,
                it.bundleName,
                it.meta
            )
        }
    }

    private suspend fun cacheBooksLinks(bookLinks: List<BookStorageEntry>) {
        bookCache.saveBooks(bookLinks)
    }
}