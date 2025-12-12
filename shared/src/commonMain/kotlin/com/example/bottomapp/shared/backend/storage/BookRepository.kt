package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import kotlinx.coroutines.flow.Flow

/**
 * Platform abstraction for book repository
 */
expect class BookRepository: IBookRepository {
    override suspend fun saveBooks(books: List<BookStorageEntry>)
    override suspend fun addBook(book: BookStorageEntry)
    override suspend fun updateBook(book: BookStorageEntry)
    override suspend fun findBook(bundleName: String): BookStorageEntry?
}

