package com.example.bottomapp.backend

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.example.bottomapp.constants.FileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class BookMetaInfo(val fileName: String, val title: String, val progress: Progress)

data class BookStorageEntry(
    val fileName: String,
    val previewUri: Uri?,
    val bookMetaInfo: BookMetaInfo
)


class BookManager(
    val context: Context,
    private val storageManager: StorageManager,
    private val bookCache: BookStorageRepository
) {
    private val _bookLinks = MutableStateFlow<List<BookStorageEntry>>(emptyList())
    val bookFlow = _bookLinks

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
        return storageManager.listAllBooks()
    }

    suspend fun refreshBooksLinks() {
        val list = getBooksLinksFromStorage()
        cacheBooksLinks(list)
    }

    suspend fun renameBookLink(oldBook: BookStorageEntry, newBook: BookStorageEntry) {
        bookCache.renameBookLink(oldBook, newBook)
    }

    private suspend fun cacheBooksLinks(bookLinks: List<BookStorageEntry>) {
        bookCache.saveBookLinks(bookLinks)
    }

    fun saveBookLinks() {
        bookFlow.value.map {
            storageManager.updateProgressData(it.bookMetaInfo)
        }
    }

    suspend fun updateBook(book: BookStorageEntry) {
        bookCache.addBook(book)
    }

    fun addNewBook(
        fileUri: Uri,
        fileName: String,
        progress: Progress,
        fileType: FileType
    ): Result<Unit> {
        storageManager.copyBookToStorage(
            fileUri,
            BookMetaInfo(fileName, fileName, progress),
            context.contentResolver,
            fileType
        ).onSuccess {
            CoroutineScope(Dispatchers.IO).launch {
                bookCache.addBook(it)
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

    suspend fun updateProgress(bookMetaInfo: BookMetaInfo) {
        val book = bookCache.findBook(bookMetaInfo.fileName)
        bookCache.updateBook(
            BookStorageEntry(
                bookMetaInfo.fileName,
                book?.previewUri,
                bookMetaInfo
            )
        )
    }
}
