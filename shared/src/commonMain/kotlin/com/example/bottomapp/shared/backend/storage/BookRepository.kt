package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import kotlinx.coroutines.flow.Flow

/**
 * Platform abstraction for book repository
 */
expect class BookRepository(
    settingsStore: PreferencesStore
) : IBookRepository {
    override suspend fun getAllBooks(): List<BookStorageEntry>
    override suspend fun getBookFlow(): Flow<List<BookStorageEntry>>
    override suspend fun saveBooks(books: List<BookStorageEntry>)
    override suspend fun addBook(book: BookStorageEntry)
    override suspend fun updateBook(book: BookStorageEntry)
    override suspend fun findBook(bundleName: String): BookStorageEntry?
    override suspend fun deleteBook(bundleName: String)
}

