package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper

actual abstract class StorageManager<T> {
    actual suspend fun updateBundlePreview(
        newBundlePreview: String,
        bundleName: String
    ): Result<String> {
        TODO("Not yet implemented")
    }

    actual suspend fun createBundle(
        bundleName: String,
        file: String,
        meta: T,
        contentResolver: ContentResolverWrapper,
        mimeType: String
    ): Result<BundleCreateResult<T>> {
        TODO("Not yet implemented")
    }

    actual suspend fun renameBundle(
        old: String,
        new: String,
        meta: T
    ): Result<BundleEditResult<T>> {
        TODO("Not yet implemented")
    }

    actual suspend fun deleteBundle(bundleName: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun updateBundleMeta(
        bundleName: String,
        meta: T
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun listAllBundles(): List<BundleEditResult<T>?> {
        TODO("Not yet implemented")
    }

    actual fun getBookFile(
        bundleName: String,
        meta: T
    ): Result<FileWrapper> {
        TODO("Not yet implemented")
    }

    actual fun clearFileCache() {
    }
}

actual class ContentResolverWrapper