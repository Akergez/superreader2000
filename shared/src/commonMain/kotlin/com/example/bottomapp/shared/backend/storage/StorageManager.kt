package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper

data class BundleCreateResult<TMeta>(
    val bookFile: String,
    val bundleName: String,
    val metaFileUri: String,
    val previewUri: String,
    val meta: TMeta,
    val author: String?
)

data class BundleEditResult<TMeta>(
    val bookFile: String,
    val bundleName: String,
    val metaFileUri: String,
    val previewUri: String,
    val meta: TMeta,
)

expect class ContentResolverWrapper

expect class FileWrapper(path: String);

expect class FileService {
    fun getFileFromPath(path: String): FileWrapper?
}

expect abstract class StorageManager<T> {

    suspend fun updateBundlePreview(newBundlePreview: String, bundleName: String): Result<String>

    suspend fun createBundle(
        bundleName: String, file: String, meta: T, mimeType: String
    ): Result<BundleCreateResult<T>>

    suspend fun renameBundle(
        old: String, new: String, meta: T
    ): Result<BundleEditResult<T>>

    suspend fun deleteBundle(bundleName: String): Result<Unit>

    suspend fun updateBundleMeta(bundleName: String, meta: T): Result<Unit>

    suspend fun listAllBundles(): List<BundleEditResult<T>?>

    fun getBookFile(bundleName: String, meta: T): Result<FileWrapper>
    fun clearFileCache()
}