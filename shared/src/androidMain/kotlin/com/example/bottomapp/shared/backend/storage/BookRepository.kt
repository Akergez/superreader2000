package com.example.bottomapp.shared.backend.storage

import android.util.Log
import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Android implementation of book repository using Room DAO
 */
actual class BookRepository actual constructor(
    private val settingsStore: PreferencesStore
) : IBookRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableListOf<BookStorageEntry>()
    private val _bookFlow = MutableStateFlow(emptyList<BookStorageEntry>())
    val bookFlow: StateFlow<List<BookStorageEntry>> = _bookFlow

    // This will be injected in actual Android implementation
    // For now, we provide a base implementation
    actual override suspend fun getAllBooks(): List<BookStorageEntry> {
        Log.d("BookRepository", "Getting all books from Android database")
        return cache
    }

    actual override suspend fun getBookFlow(): Flow<List<BookStorageEntry>> {
        return kotlinx.coroutines.flow.flowOf(cache)
    }

    actual override suspend fun saveBooks(books: List<BookStorageEntry>) {
        Log.d("BookRepository", "Saving ${books.size} books")
        cache.clear()
        cache.addAll(books)
    }

    actual override suspend fun addBook(book: BookStorageEntry) {
        Log.d("BookRepository", "Adding book: ${book.bundleName}")
        cache.add(book)
    }

    actual override suspend fun updateBook(book: BookStorageEntry) {
        Log.d("BookRepository", "Updating book: ${book.bundleName}")
        val index = cache.indexOfFirst { it.bundleName == book.bundleName }
        if (index >= 0) {
            cache[index] = book
        }
    }

    actual override suspend fun findBook(bundleName: String): BookStorageEntry? {
        Log.d("BookRepository", "Finding book: $bundleName")
        return cache.find { it.bundleName == bundleName }
    }

    actual override suspend fun deleteBook(bundleName: String) {
        Log.d("BookRepository", "Deleting book: $bundleName")
        cache.removeAll { it.bundleName == bundleName }
    }
}

