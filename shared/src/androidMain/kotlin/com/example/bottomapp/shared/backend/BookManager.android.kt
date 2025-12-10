package com.example.bottomapp.shared.backend

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import coil.ImageLoader
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
    val context: Context,
    private val storageManager: BookStorage,
    private val bookCache: BookRepository,
    private val imageBuilder: ImageLoader
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
            ?: return Result.failure(Exception(""))
        return storageManager.getBookFile(bundleName, meta.bookMetaInfo)
    }

    actual suspend fun addNewBook(
        fileUri: String, fileName: String, progress: Progress, fileType: FileType
    ): Result<Unit> {
        storageManager.createBundle(
            fileName,
            fileUri,
            BookMetaInfo(fileName, progress),
            ContentResolverWrapper(context.contentResolver),
            fileType.toFileExtension()
        ).onSuccess {
            CoroutineScope(Dispatchers.IO).launch {
                bookCache.addBook(
                    BookStorageEntry(
                        fileName, it.previewUri.toString(), fileName, null, it.meta
                    )
                )
            }
            return Result.success(Unit)
        }.onFailure {
            Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
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
                Result.success(Unit)
            }.onFailure {
                Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    actual suspend fun renameBook(bundleName: String, newNameForBundle: String): Result<Unit> {
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("getString(context, R.string.exception_book_not_found)"))

        storageManager.renameBundle(bundleName, newNameForBundle, meta.bookMetaInfo)
            .onSuccess {
                storageManager.updateBundleMeta(
                    newNameForBundle,
                    BookMetaInfo(newNameForBundle, meta.bookMetaInfo.progress)
                ).onSuccess {
                    refreshBooksLinks()
                    Result.success(Unit)
                }.onFailure {
                    storageManager.renameBundle(newNameForBundle, bundleName, meta.bookMetaInfo)
                    Result.failure<Unit>(it)
                }
            }
            .onFailure {
                Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    actual suspend fun updateAuthor(bundleName: String, author: String?): Result<Unit> {
        Log.d("BookManager", "updateAuthor: $author")
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("getString(context, R.string.exception_book_not_found)"))

        storageManager.updateBundleMeta(bundleName, meta.bookMetaInfo.copy(author = author))
            .onSuccess {
                refreshBooksLinks()
                Result.success(Unit)
            }
            .onFailure {
                Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    actual suspend fun updateBookPreview(bundleName: String, newPreviewUri: String): Result<String> {
        val newPreview = storageManager.updateBundlePreview(newPreviewUri, bundleName)
        newPreview.onSuccess {
            refreshBooksLinks()
            imageBuilder.memoryCache?.clear()
            Result.success(it)
        }.onFailure {
            Result.failure<Uri>(it.cause!!)
        }
        return Result.failure(Exception("getString(context, R.string.exception_update_preview)"))
    }

    actual suspend fun saveBookLinks() {
        Log.i("BookManager", "saveBookLinks")
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
                it.previewUri.toString(),
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