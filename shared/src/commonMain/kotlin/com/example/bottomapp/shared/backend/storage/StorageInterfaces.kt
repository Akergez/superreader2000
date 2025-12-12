package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import kotlinx.serialization.Serializable

/**
 * Book storage entry with metadata and preview
 */
@Serializable
data class BookStorageEntry(
    val title: String,
    val previewUri: String?,
    val bundleName: String,
    val progressInPercent: Double?,
    val bookMetaInfo: BookMetaInfo
)

/**
 * Book repository interface for database operations
 */
interface IBookRepository {
    suspend fun saveBooks(books: List<BookStorageEntry>)
    suspend fun addBook(book: BookStorageEntry)
    suspend fun updateBook(book: BookStorageEntry)
    suspend fun findBook(bundleName: String): BookStorageEntry?
}
