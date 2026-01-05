package com.akergez.superreader2000.shared.backend.storage

import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import kotlinx.serialization.Serializable

@Serializable
data class BookStorageEntry(
    val title: String,
    val previewUri: String?,
    val bundleName: String,
    val bookMetaInfo: BookMetaInfo
)

interface IBookRepository {
    suspend fun saveBooks(books: List<BookStorageEntry>)
    suspend fun addBook(book: BookStorageEntry)
    suspend fun updateBook(book: BookStorageEntry)
    suspend fun findBook(bundleName: String): BookStorageEntry?
}
