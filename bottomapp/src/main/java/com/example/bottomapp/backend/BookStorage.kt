package com.example.bottomapp.backend

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.bottomapp.constants.FileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class BookMetaInfo(val fileName: String, val title: String, val progress: Progress)

data class BookStorageEntry(val fileName: String, val previewUri: Uri?, val bookMetaInfo: BookMetaInfo)


class BookStorage(val context: Context,
                  private val storageManager: StorageManager,
                  private val bookRepository: BookStorageRepository
) {
    val bookCache: SnapshotStateList<BookStorageEntry> = mutableStateListOf()
    val booksHashMap: HashSet<String> = HashSet()

    init {
        refreshBooksLinksFromCache()
    }

    suspend fun getBooksLinksFromStorage(): List<BookStorageEntry> {
        return storageManager.listAllBooks()
    }

    private fun getBooksLinksFromCache(): List<BookStorageEntry> {
        return runBlocking {
            bookRepository.getBookLinks()
        }
    }

    suspend fun refreshBooksLinks() {
        val list = getBooksLinksFromStorage()
        if (bookCache.toSet() == list.toSet()) return
        bookCache.clear()
        bookCache.addAll(list)
        cacheBooksLinks()
    }

    private fun refreshBooksLinksFromCache() {
        bookCache.clear()
        bookCache.addAll(getBooksLinksFromCache())
    }

    suspend fun saveBooksLinksToStorage() {
        bookCache.filter { booksHashMap.contains(it.bookMetaInfo.fileName) }
            .map { bookLink -> storageManager.updateProgressData(bookLink.bookMetaInfo) }
        booksHashMap.clear()
        cacheBooksLinks()
    }

    private suspend fun cacheBooksLinks() {
        bookRepository.saveBookLinks(bookCache)
    }

    fun addNewBook(fileUri: Uri, fileName: String, progress: Progress, fileType: FileType): Result<Unit> {
        storageManager.copyBookToStorage(
            fileUri,
            BookMetaInfo(fileName, fileName, progress),
            context.contentResolver,
            fileType
        ).onSuccess {
            bookCache.add(it)
            CoroutineScope(Dispatchers.IO).launch {
                bookRepository.addBook(it)
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

    fun updateProgress(bookMetaInfo: BookMetaInfo) {
        val exist = bookCache.find { it.bookMetaInfo.fileName == bookMetaInfo.fileName }
        if (exist == null) {
            return
        }

        bookCache.remove(exist)
        bookCache.add(BookStorageEntry(exist.fileName, exist.previewUri, bookMetaInfo))
        booksHashMap.add(exist.bookMetaInfo.fileName)
    }
}
