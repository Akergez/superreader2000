package com.example.bottomapp.shared.backend

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.Progress
import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.backend.storage.BookRepository
import com.example.bottomapp.shared.backend.storage.BookStorageEntry
import com.example.bottomapp.shared.backend.storage.ContentResolverWrapper
import com.example.bottomapp.shared.constants.FileType
import com.example.bottomapp.shared.constants.toFileExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

actual class BookManager(
    private val storageManager: BookStorage,
    private val bookCache: BookRepository,
    private val messageHandler: com.example.bottomapp.shared.platform.MessageHandler
) {
    private val _bookLinks = MutableStateFlow<List<BookStorageEntry>>(emptyList())
    actual val bookFlow = _bookLinks.asStateFlow()
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

    actual suspend fun getBookFile(bundleName: String): Result<FileWrapper> {
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))
        return storageManager.getBookFile(bundleName, meta.bookMetaInfo)
    }

    actual suspend fun addNewBook(
        fileUri: String,
        fileName: String,
        progress: Progress,
        fileType: FileType
    ): Result<Unit> {
        storageManager.createBundle(
            fileName,
            fileUri,
            BookMetaInfo(fileName, progress),
            ContentResolverWrapper(),
            fileType.toFileExtension()
        ).onSuccess {
            CoroutineScope(Dispatchers.IO).launch {
                bookCache.addBook(
                    BookStorageEntry(
                        fileName, it.previewUri, fileName, null, it.meta
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

    actual suspend fun refreshBooksLinks() {
        val list = getBooksLinksFromStorage()
        cacheBooksLinks(list)
    }

    actual suspend fun updateProgress(bundleName: String, bookMetaInfo: BookMetaInfo) {
        val book = bookCache.findBook(bookMetaInfo.title)
        bookCache.updateBook(
            BookStorageEntry(
                bookMetaInfo.title,
                book?.previewUri,
                bundleName,
                bookMetaInfo.progress!!.getProgressInPercent(),
                bookMetaInfo
            )
        )
        _pendingBooksToSave.add(bundleName)
    }

    actual suspend fun deleteBook(bundleName: String): Result<Unit> {
        storageManager.deleteBundle(bundleName)
            .onSuccess {
                refreshBooksLinks()
                return Result.success(Unit)
            }.onFailure {
                return Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    actual suspend fun renameBook(bundleName: String, newNameForBundle: String): Result<Unit> {
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

    actual suspend fun updateAuthor(bundleName: String, author: String?): Result<Unit> {
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

    actual suspend fun updateBookPreview(bundleName: String, newPreviewUri: String): Result<String> {
        val newPreview = storageManager.updateBundlePreview(newPreviewUri, bundleName)
        newPreview.onSuccess {
            refreshBooksLinks()
            return Result.success(it)
        }.onFailure {
            return Result.failure<String>(it.cause ?: it)
        }
        return Result.failure(Exception("Failed to update preview"))
    }

    actual suspend fun saveBookLinks() {
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
                it.meta.progress!!.getProgressInPercent(),
                it.meta
            )
        }
    }

    private suspend fun cacheBooksLinks(bookLinks: List<BookStorageEntry>) {
        bookCache.saveBooks(bookLinks)
    }
}


