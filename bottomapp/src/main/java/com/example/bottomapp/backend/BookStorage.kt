package com.example.bottomapp.backend

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

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

    private suspend fun getBooksLinksFromStorage(): List<BookStorageEntry> {
        return storageManager.listAllBooks()
    }

    private fun getBooksLinksFromCache(): List<BookStorageEntry> {
        return bookRepository.getBookLinks()
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

    fun saveBooksLinksToStorage() {
        bookCache.filter { booksHashMap.contains(it.bookMetaInfo.fileName) }
            .map { bookLink -> storageManager.updateProgressData(bookLink.bookMetaInfo) }
        booksHashMap.clear()
        cacheBooksLinks()
    }

    private fun cacheBooksLinks() {
        bookRepository.saveBookLinks(bookCache)
    }

    fun addNewBook(fileUri: Uri, fileName: String, progress: Progress)
    {
       val storageEntry = storageManager.copyBookToStorage(
            fileUri,
            BookMetaInfo(fileName, fileName, progress),
            context.contentResolver
        )
        bookCache.add(storageEntry)
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