package com.example.bottomapp.backend

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.bottomapp.constants.FileType
import com.example.bottomapp.constants.toMimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class BookMetaInfo(val title: String, val progress: Progress, val author: String?)

data class BookStorageEntry(
    val title: String,
    val previewUri: Uri?,
    val bundleName: String,
    val progressInPercent: Double?,
    val bookMetaInfo: BookMetaInfo
)


class BookManager(
    val context: Context,
    private val storageManager: BookStorage,
    private val bookCache: BookStorageRepository
) {
    private val _bookLinks = MutableStateFlow<List<BookStorageEntry>>(emptyList())
    val bookFlow = _bookLinks
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

    suspend fun getBookFile(bundleName: String): Result<File> {
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))
        return storageManager.getBookFile(bundleName, meta.bookMetaInfo)
    }

    suspend fun addNewBook(
        fileUri: Uri, fileName: String, progress: Progress, fileType: FileType
    ): Result<Unit> {
        val shortFileName = fileName.substring(0, fileName.length - 4)
        storageManager.createBundle(
            fileName,
            fileUri,
            BookMetaInfo(shortFileName, progress, null),
            context.contentResolver,
            fileType.toMimeType()
        ).onSuccess {
            CoroutineScope(Dispatchers.IO).launch {
                bookCache.addBook(
                    BookStorageEntry(
                        shortFileName, it.previewUri, fileName, null, it.meta
                    )
                )
            }
            return Result.success(Unit)
        }.onFailure {
            Toast.makeText(context, "Book already exists", Toast.LENGTH_LONG).show()
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
                bookMetaInfo.title, book?.previewUri, bundleName, bookMetaInfo.progress.getProgressInPercent(), bookMetaInfo
            )
        )
        _pendingBooksToSave.add(bundleName)
    }

    suspend fun deleteBook(bundleName: String): Result<Unit> {
        storageManager.deleteBundle(bundleName)
            .onSuccess {
                refreshBooksLinks()
                Result.success(Unit)
            }.onFailure {
                Result.failure<Unit>(it.cause!!)
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
                    BookMetaInfo(newNameForBundle, meta.bookMetaInfo.progress, null)
                ).onSuccess {
                    refreshBooksLinks()
                    Result.success(Unit)
                }.onFailure {
                    storageManager.renameBundle(newNameForBundle, bundleName, meta.bookMetaInfo)
                    Result.failure<Unit>(it.cause!!)
                }
            }
            .onFailure {
                Result.failure<Unit>(it.cause ?: Exception("Failed to rename book"))
            }
        return Result.success(Unit)
    }

    suspend fun updateAuthor(bundleName: String, author: String?): Result<Unit> {
        Log.d("BookManager", "updateAuthor: $author")
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))

        storageManager.updateBundleMeta(bundleName, meta.bookMetaInfo.copy(author = author))
            .onSuccess {
                    refreshBooksLinks()
                    Result.success(Unit)
            }
            .onFailure {
                Result.failure<Unit>(it.cause ?: Exception("Failed to update author"))
            }
        return Result.success(Unit)
    }

    suspend fun updateBookPreview(bundleName: String, newPreviewUri: Uri): Result<Uri> {
        val newPreview = storageManager.updateBundlePreview(newPreviewUri, bundleName)
        newPreview.onSuccess {
            refreshBooksLinks()
            Result.success(it)
        }.onFailure {
            Result.failure<Uri>(it.cause!!)
        }
        return Result.failure(Exception("Failed to update preview"))
    }

    suspend fun saveBookLinks() {
        Log.i("BookManager", "saveBookLinks")
        bookFlow.value.map {
            if (_pendingBooksToSave.contains(it.bundleName)) {
                _pendingBooksToSave.remove(it.bundleName)
                storageManager.updateBundleMeta(it.bundleName, it.bookMetaInfo)
            }
        }
    }

    private suspend fun getBooksLinksFromStorage(): List<BookStorageEntry> {
        return storageManager.listAllBundles().map {
            BookStorageEntry(
                it?.meta?.title!!, it.previewUri, it.bundleName, it.meta.progress.getProgressInPercent(), it.meta
            )
        }
    }

    private suspend fun cacheBooksLinks(bookLinks: List<BookStorageEntry>) {
        bookCache.saveBookLinks(bookLinks)
    }
}
