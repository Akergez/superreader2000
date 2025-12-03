package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import kotlinx.serialization.Serializable

/**
 * Result of creating a new book bundle
 */
@Serializable
data class BundleCreateResult(
    val bundlePath: String,
    val bundleName: String,
    val metaFilePath: String,
    val previewPath: String,
    val meta: BookMetaInfo,
    val author: String?
)

/**
 * Result of editing a book bundle
 */
@Serializable
data class BundleEditResult(
    val bookFilePath: String,
    val bundleName: String,
    val metaFilePath: String,
    val previewPath: String,
    val meta: BookMetaInfo,
)

/**
 * Book storage entry with metadata and preview
 */
@Serializable
data class BookStorageEntry(
    val title: String,
    val bundleName: String,
    val progressInPercent: Double?,
    val previewUri: String?,
    val bookMetaInfo: BookMetaInfo
)

/**
 * Serialization/deserialization interface
 */
interface IDeserializer<T> {
    fun deserialize(jsonString: String): T
    fun serialize(obj: T): String
}

/**
 * Filename provider interface
 */
interface INameProvider<T> {
    fun jsonMetaFilename(bundleName: String): String
    fun previewFilename(bundleName: String): String
    fun fileName(bundleName: String, meta: T): String
}

/**
 * Settings storage interface
 */
interface ISettingsStore {
    suspend fun getString(key: String, default: String = ""): String
    suspend fun putString(key: String, value: String)
    suspend fun getInt(key: String, default: Int = 0): Int
    suspend fun putInt(key: String, value: Int)
    suspend fun getBoolean(key: String, default: Boolean = false): Boolean
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun removeKey(key: String)
}

/**
 * Book repository interface for database operations
 */
interface IBookRepository {
    suspend fun getAllBooks(): List<BookStorageEntry>
    suspend fun getBookFlow(): kotlinx.coroutines.flow.Flow<List<BookStorageEntry>>
    suspend fun saveBooks(books: List<BookStorageEntry>)
    suspend fun addBook(book: BookStorageEntry)
    suspend fun updateBook(book: BookStorageEntry)
    suspend fun findBook(bundleName: String): BookStorageEntry?
    suspend fun deleteBook(bundleName: String)
}

/**
 * Storage manager interface for file operations
 */
interface IStorageManager {
    suspend fun createBundle(
        bundleName: String,
        filePath: String,
        meta: BookMetaInfo,
        mimeType: String
    ): Result<BundleCreateResult>

    suspend fun deleteBundle(bundleName: String): Result<Unit>
    suspend fun updateBundleMeta(bundleName: String, meta: BookMetaInfo): Result<Unit>
    suspend fun updateBundlePreview(newPreviewPath: String, bundleName: String): Result<String>
    suspend fun renameBundle(old: String, new: String, meta: BookMetaInfo): Result<BundleEditResult>
    suspend fun listAllBundles(): List<BundleEditResult>?
    suspend fun getBookFilePath(bundleName: String, meta: BookMetaInfo): Result<String>
}

