package com.example.bottomapp.shared.backend.storage

import java.io.File

actual class BookRepository(
    private val settingsStore: PreferencesStore
) : IBookRepository {
    private val cache = mutableListOf<BookStorageEntry>()
    private val storagePath get() = System.getProperty("user.home") + "/.config/pdfreaderr/books"

    init {
        File(storagePath).mkdirs()
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
}

