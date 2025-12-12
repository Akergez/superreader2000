package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.exceptions.InvalidStorageDirectoryException
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.bookExtractors.IBookInfoExtractorFactory
import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO

actual abstract class StorageManager<T>(
    private val settingsManager: ISettingsManager,
    private val deserializer: IDeserializer<T>,
    private val nameProvider: INameProvider<T>,
    private val infoExtractor: IBookInfoExtractorFactory<T>,
    private val previewGeneratorProvider: (T) -> IPreviewGenerator
) {
    private val fileCache = HashMap<String, File>()

    actual suspend fun updateBundlePreview(
        newBundlePreview: String,
        bundleName: String
    ): Result<String> =
        withContext(Dispatchers.IO) {
            val storagePath = settingsManager.getStoragePath() ?: return@withContext Result.failure(
                Exception("Storage directory not set")
            )
            val storageDir = File(storagePath)
            if (!storageDir.exists() || !storageDir.isDirectory) {
                return@withContext Result.failure(Exception("Invalid storage directory"))
            }
            val bundleDir = File(storageDir, bundleName)
            if (!bundleDir.exists() || !bundleDir.isDirectory) {
                return@withContext Result.failure(Exception("Bundle not found"))
            }
            val fileName = nameProvider.previewFilename(bundleName)
            val previewFile = File(bundleDir, fileName)

            val sourceFile = File(newBundlePreview)
            sourceFile.copyTo(previewFile, overwrite = true)

            return@withContext Result.success(previewFile.absolutePath)
        }

    actual suspend fun createBundle(
        bundleName: String,
        file: String,
        meta: T,
        contentResolver: ContentResolverWrapper,
        mimeType: String
    ): Result<BundleCreateResult<T>> = withContext(Dispatchers.IO) {
        val storagePath = settingsManager.getStoragePath() ?: return@withContext Result.failure(
            Error("Storage directory not set")
        )
        val storageDir = File(storagePath)
        if (!storageDir.exists() || !storageDir.isDirectory) {
            return@withContext Result.failure(Error("Invalid storage directory"))
        }

        val bundleDir = File(storageDir, bundleName)
        if (bundleDir.exists()) {
            return@withContext Result.failure(Error("Bundle already exists"))
        }

        if (!bundleDir.mkdirs()) {
            return@withContext Result.failure(Error("Failed to create bundle directory"))
        }

        val destFile = File(bundleDir, nameProvider.fileName(bundleName, meta))
        val sourceFile = File(file.removePrefix("file:"))
        sourceFile.copyTo(destFile, overwrite = false)

        val metaFile = File(bundleDir, nameProvider.jsonMetaFilename(bundleName))
        val jsonString = deserializer.serialize(meta)
        metaFile.writeText(jsonString)

        val previewFile = File(bundleDir, nameProvider.previewFilename(bundleName))
        var bookFile: File? = null
        getBookFile(bundleName, meta).onSuccess { bookFile = it.file }
            .onFailure { return@withContext Result.failure(it) }

        val previewImage = previewGeneratorProvider(meta).generatePreview(FileWrapper(bookFile!!))
            ?: return@withContext Result.failure(Error("Failed to generate preview bitmap"))

        ImageIO.write(previewImage.bufferedImage, "jpeg", previewFile)

        return@withContext Result.success(
            BundleCreateResult<T>(
                destFile.absolutePath,
                bundleName,
                metaFile.absolutePath,
                previewFile.absolutePath,
                meta,
                infoExtractor.createExtractor(meta).getAuthor(bookFile!!)
            )
        )
    }

    actual suspend fun renameBundle(
        old: String, new: String, meta: T
    ): Result<BundleEditResult<T>> = withContext(Dispatchers.IO) {
        val storagePath = settingsManager.getStoragePath() ?: return@withContext Result.failure(
            Exception("Storage directory not set")
        )
        val storageDir = File(storagePath)
        if (!storageDir.exists() || !storageDir.isDirectory) {
            return@withContext Result.failure(Exception("Invalid storage directory"))
        }

        val bundleDir = File(storageDir, old)
        if (!bundleDir.exists() || !bundleDir.isDirectory) {
            return@withContext Result.failure(Exception("Bundle not found"))
        }

        val newBundleDir = File(storageDir, new)
        if (!bundleDir.renameTo(newBundleDir)) {
            return@withContext Result.failure(Exception("Failed to rename bundle directory"))
        }

        val oldJsonFile = File(newBundleDir, nameProvider.jsonMetaFilename(old))
        val oldImageFile = File(newBundleDir, nameProvider.previewFilename(old))
        val oldBookFile = File(newBundleDir, nameProvider.fileName(old, meta))

        val newJsonFile = File(newBundleDir, nameProvider.jsonMetaFilename(new))
        val newImageFile = File(newBundleDir, nameProvider.previewFilename(new))
        val newBookFile = File(newBundleDir, nameProvider.fileName(new, meta))

        oldJsonFile.renameTo(newJsonFile)
        oldImageFile.renameTo(newImageFile)
        oldBookFile.renameTo(newBookFile)

        return@withContext Result.success(
            BundleEditResult(
                newBookFile.absolutePath,
                new,
                newJsonFile.absolutePath,
                newImageFile.absolutePath,
                meta,
            )
        )
    }

    actual suspend fun deleteBundle(bundleName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val storagePath = settingsManager.getStoragePath() ?: return@withContext Result.failure(
                Exception("Storage directory not set")
            )
            val storageDir = File(storagePath)
            if (!storageDir.exists() || !storageDir.isDirectory) {
                return@withContext Result.failure(InvalidStorageDirectoryException())
            }

            val bundleDir = File(storageDir, bundleName)
            if (!bundleDir.exists()) {
                return@withContext Result.failure(Exception("Bundle not found"))
            }

            bundleDir.deleteRecursively()

            return@withContext Result.success(Unit)
        }

    actual suspend fun updateBundleMeta(bundleName: String, meta: T): Result<Unit> =
        withContext(Dispatchers.IO) {
            val storagePath = settingsManager.getStoragePath() ?: return@withContext Result.failure(
                Exception("Storage directory not set")
            )
            val storageDir = File(storagePath)
            if (!storageDir.exists() || !storageDir.isDirectory) {
                return@withContext Result.failure(InvalidStorageDirectoryException())
            }

            val bundleDir = File(storageDir, bundleName)
            if (!bundleDir.exists() || !bundleDir.isDirectory) {
                return@withContext Result.failure(Exception("Bundle not found"))
            }

            val metaFile = File(bundleDir, nameProvider.jsonMetaFilename(bundleName))
            if (!metaFile.exists()) {
                return@withContext Result.failure(Exception("Metadata file not found"))
            }

            val jsonString = deserializer.serialize(meta)
            metaFile.writeText(jsonString)

            return@withContext Result.success(Unit)
        }

    actual suspend fun listAllBundles(): List<BundleEditResult<T>?> = coroutineScope {
        val storagePath = settingsManager.getStoragePath() ?: return@coroutineScope emptyList()
        val storageDir = File(storagePath)
        if (!storageDir.exists() || !storageDir.isDirectory) {
            return@coroutineScope emptyList()
        }

        val files = storageDir.listFiles() ?: return@coroutineScope emptyList()
        files.map { file ->
            async(Dispatchers.IO) {
                if (file.isDirectory) {
                    val metaFile = File(file, nameProvider.jsonMetaFilename(file.name))
                    if (metaFile.exists()) {
                        val jsonString = metaFile.readText()
                        val bookMetaInfo = deserializer.deserialize(jsonString)
                        val previewFile = File(file, nameProvider.previewFilename(file.name))
                        val bookFile = File(file, nameProvider.fileName(file.name, bookMetaInfo))
                        if (!bookFile.exists()) {
                            return@async null
                        }
                        BundleEditResult(
                            bookFile.absolutePath,
                            file.name,
                            metaFile.absolutePath,
                            previewFile.absolutePath,
                            bookMetaInfo
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }.awaitAll()
    }

    actual fun getBookFile(bundleName: String, meta: T): Result<FileWrapper> {
        // Check if the file is already in the cache
        fileCache[bundleName]?.let {
            return Result.success(FileWrapper(it))
        }

        val storagePath = settingsManager.getStoragePath()
            ?: return Result.failure(Exception("Storage directory not set"))
        val storageDir = File(storagePath)
        if (!storageDir.exists() || !storageDir.isDirectory) {
            return Result.failure(InvalidStorageDirectoryException())
        }

        val bundleDir = File(storageDir, bundleName)
        if (!bundleDir.exists() || !bundleDir.isDirectory) {
            return Result.failure(Exception("Bundle not found"))
        }

        val bookFile = File(bundleDir, nameProvider.fileName(bundleName, meta))
        if (!bookFile.exists()) {
            return Result.failure(Exception("File not found in storage directory"))
        }

        // Cache the file
        fileCache[bundleName] = bookFile

        return Result.success(FileWrapper(bookFile))
    }

    actual fun clearFileCache() {
        fileCache.clear()
    }
}

actual class ContentResolverWrapper