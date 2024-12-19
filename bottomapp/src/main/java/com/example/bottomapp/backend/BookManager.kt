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

data class BookMetaInfo(val title: String, val progress: Progress)

data class BookStorageEntry(
    val title: String,
    val previewUri: Uri?,
    val bundleName: String,
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

    private suspend fun getBooksLinksFromStorage(): List<BookStorageEntry> {
        return storageManager.listAllBundles()
            .map {
                BookStorageEntry(
                    it?.meta?.title!!,
                    it.previewUri,
                    it.bundleName,
                    it.meta
                )
            }
    }

    suspend fun refreshBooksLinks() {
        val list = getBooksLinksFromStorage()
        cacheBooksLinks(list)
    }

    private suspend fun cacheBooksLinks(bookLinks: List<BookStorageEntry>) {
        bookCache.saveBookLinks(bookLinks)
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

    suspend fun addNewBook(
        fileUri: Uri,
        fileName: String,
        progress: Progress,
        fileType: FileType
    ): Result<Unit> {
        storageManager.createBundle(
            fileName,
            fileUri,
            BookMetaInfo(fileName, progress),
            context.contentResolver,
            fileType.toMimeType()
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
            Toast
                .makeText(context, "Book already exists", Toast.LENGTH_LONG)
                .show()
            return Result.failure(it)
        }
        return Result.success(Unit)
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
}
