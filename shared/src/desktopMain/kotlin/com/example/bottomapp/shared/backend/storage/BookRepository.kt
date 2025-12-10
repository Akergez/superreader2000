package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Desktop implementation of book repository using file-based storage
 */
actual class BookRepository actual constructor(
    private val settingsStore: PreferencesStore
) : IBookRepository {
    private val cache = mutableListOf<BookStorageEntry>()
    private val storagePath get() = System.getProperty("user.home") + "/.config/pdfreaderr/books"

    init {
        File(storagePath).mkdirs()
    }

    actual override suspend fun getAllBooks(): List<BookStorageEntry> {
        return cache
    }

    actual override suspend fun getBookFlow(): Flow<List<BookStorageEntry>> {
        return flowOf(cache)
    }

    actual override suspend fun saveBooks(books: List<BookStorageEntry>) {
        cache.clear()
        cache.addAll(books)
    }

    actual override suspend fun addBook(book: BookStorageEntry) {
        cache.add(book)
    }

    actual override suspend fun updateBook(book: BookStorageEntry) {
        val index = cache.indexOfFirst { it.bundleName == book.bundleName }
        if (index >= 0) {
            cache[index] = book
        }
    }

    actual override suspend fun findBook(bundleName: String): BookStorageEntry? {
        return cache.find { it.bundleName == bundleName }
    }

    actual override suspend fun deleteBook(bundleName: String) {
        cache.removeAll { it.bundleName == bundleName }
    }
}

