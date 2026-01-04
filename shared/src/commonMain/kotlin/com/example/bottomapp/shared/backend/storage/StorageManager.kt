package com.example.bottomapp.shared.backend.storage

import com.example.bottomapp.shared.backend.exceptions.FailedToCreateFileException
import com.example.bottomapp.shared.backend.exceptions.InvalidStorageDirectoryException
import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.Image
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.formats.abstractions.IBookInfoExtractorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

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

expect class DocumentFileWrapper {
    val name: String?
    fun findFile(name: String): DocumentFileWrapper?
    fun absolutePath(): String
    fun createDirectory(name: String): DocumentFileWrapper?
    fun createFile(mimeType: String, name: String): DocumentFileWrapper?
    fun renameTo(name: String): Boolean
    fun delete(): Boolean
    fun listFiles(): List<DocumentFileWrapper>
    fun isDirectory(): Boolean
    fun exists(): Boolean
}

expect class FileService {
    fun getFileFromPath(path: String): DocumentFileWrapper?
    fun copy(source: String, destination: String)
    fun writeText(path: String, content: String)
    fun readText(path: String): String?
    fun writeBitmap(path: String, bitmap: Image)
    fun copyToLocalFile(source: DocumentFileWrapper, destFileName: String): File
}

abstract class StorageManager<T>(
    private val settingsManager: ISettingsManager,
    private val deserializer: IDeserializer<T>,
    private val nameProvider: INameProvider<T>,
    private val infoExtractor: IBookInfoExtractorFactory<T>,
    private val previewGeneratorProvider: (T) -> IPreviewGenerator,
    private val fileService: FileService
) {
    private val fileCache = HashMap<String, File>()

    suspend fun updateBundlePreview(
        newBundlePreview: String, bundleName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val storageUri = settingsManager.getStoragePath() ?: return@withContext Result.failure(
            Exception("Storage directory not set")
        )
        val storageDir =
            fileService.getFileFromPath(storageUri) ?: return@withContext Result.failure(
                Exception("Invalid storage directory")
            )
        val bundleDir = storageDir.findFile(bundleName) ?: return@withContext Result.failure(
            Exception("Bundle not found")
        )
        val fileName = nameProvider.previewFilename(bundleName)

        val previewFile = bundleDir.findFile(fileName) ?: return@withContext Result.failure(
            Exception("Failed to create preview file")
        )

        val sourceFile =
            fileService.getFileFromPath(newBundlePreview) ?: return@withContext Result.failure(
                Exception("Invalid preview file")
            )
        fileService.copy(sourceFile.absolutePath(), previewFile.absolutePath())

        return@withContext Result.success(previewFile.absolutePath())
    }

    suspend fun createBundle(
        bundleName: String, file: String, meta: T, mimeType: String
    ): Result<BundleCreateResult<T>> = withContext(Dispatchers.IO) {
        val storageUri =
            settingsManager.getStoragePath() ?: return@withContext Result.failure(
                Error("Storage directory not set")
            )
        val storageDir =
            fileService.getFileFromPath(storageUri) ?: return@withContext Result.failure(
                Error("Invalid storage directory")
            )

        val existingFile = storageDir.findFile(bundleName)
        if (existingFile != null) {
            return@withContext Result.failure(Error("Bundle already exists"))
        }

        val bundleStorage = storageDir.createDirectory(bundleName)
            ?: return@withContext Result.failure(Error("Failed to create bundle directory"))

        val destFile = bundleStorage.createFile(mimeType, nameProvider.fileName(bundleName, meta))
            ?: return@withContext Result.failure(
                Error("Failed to create destination file")
            )

        fileService.copy(file, destFile.absolutePath())

        val metaFile =
            bundleStorage.createFile("application/json", nameProvider.jsonMetaFilename(bundleName))
                ?: return@withContext Result.failure(
                    FailedToCreateFileException()
                )

        val jsonString = deserializer.serialize(meta)
        fileService.writeText(metaFile.absolutePath(), jsonString)

        val previewFile =
            bundleStorage.createFile("image/jpeg", nameProvider.previewFilename(bundleName))
                ?: return@withContext Result.failure(
                    Error("Failed to create preview file")
                )

        var bookFile: File? = null
        getBookFile(bundleName, meta).onSuccess { bookFile = it.file }
            .onFailure { return@withContext Result.failure(it) }

        val previewBitmap =
            previewGeneratorProvider(meta).generatePreview(FileWrapper(bookFile!!))
                ?: return@withContext Result.failure(
                    Error("Failed to generate preview bitmap")
                )

        fileService.writeBitmap(previewFile.absolutePath(), previewBitmap)

        return@withContext Result.success(
            BundleCreateResult<T>(
                destFile.absolutePath(),
                bundleName,
                metaFile.absolutePath(),
                previewFile.absolutePath(),
                meta,
                infoExtractor.createExtractor(meta).getAuthor(bookFile!!)
            )
        )
    }

    suspend fun renameBundle(
        old: String, new: String, meta: T
    ): Result<BundleEditResult<T>> = withContext(Dispatchers.IO) {
        val storageUri =
            settingsManager.getStoragePath() ?: return@withContext Result.failure(
                Exception("Storage directory not set")
            )
        val storageDir = fileService.getFileFromPath(storageUri)

        val bundleDir = storageDir?.findFile(old)
            ?: return@withContext Result.failure(Exception("Bundle not found"))

        if (!bundleDir.renameTo(new)) {
            return@withContext Result.failure(Exception("Failed to rename bundle directory"))
        }

        val oldJsonPath = bundleDir.findFile(nameProvider.jsonMetaFilename(old))
            ?: return@withContext Result.failure(Exception("Metadata file not found"))
        val oldImagePath = bundleDir.findFile(nameProvider.previewFilename(old))
            ?: return@withContext Result.failure(Exception("Preview file not found"))
        val oldFilePath = bundleDir.findFile(nameProvider.fileName(old, meta))
            ?: return@withContext Result.failure(Exception("Book file not found"))

        oldJsonPath.renameTo(nameProvider.jsonMetaFilename(new))
        oldImagePath.renameTo(nameProvider.previewFilename(new))
        oldFilePath.renameTo(nameProvider.fileName(new, meta))

        return@withContext Result.success(
            BundleEditResult(
                oldFilePath.absolutePath(),
                new,
                oldJsonPath.absolutePath(),
                oldImagePath.absolutePath(),
                meta,
            )
        )
    }

    suspend fun deleteBundle(bundleName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val storageUri =
                settingsManager.getStoragePath() ?: return@withContext Result.failure(
                    Exception("Storage directory not set")
                )
            val storageDir = fileService.getFileFromPath(storageUri) ?: return@withContext Result.failure(
                InvalidStorageDirectoryException()
            )

            val bundleDir = storageDir.findFile(bundleName) ?: return@withContext Result.failure(
                Exception("Bundle not found")
            )

            bundleDir.delete()

            return@withContext Result.success(Unit)
        }

    suspend fun updateBundleMeta(bundleName: String, meta: T): Result<Unit> =
        withContext(Dispatchers.IO) {
            val storageUri =
                settingsManager.getStoragePath() ?: return@withContext Result.failure(
                    Exception("Storage directory not set")
                )
            val storageDir = fileService.getFileFromPath(storageUri) ?: return@withContext Result.failure(
                Exception("Invalid storage directory")
            )

            val bundleDir = storageDir.findFile(bundleName) ?: return@withContext Result.failure(
                Exception("Bundle not found")
            )

            val metaFile = bundleDir.findFile(nameProvider.jsonMetaFilename(bundleName))
                ?: return@withContext Result.failure(
                    Exception("Metadata file not found")
                )
            metaFile.delete()
            val updatedMetaFile =
                bundleDir.createFile("application/json", nameProvider.jsonMetaFilename(bundleName))
                    ?: return@withContext Result.failure(
                        Exception("Failed to create metadata file")
                    )

            val jsonString = deserializer.serialize(meta)
            fileService.writeText(updatedMetaFile.absolutePath(), jsonString)

            return@withContext Result.success(Unit)
        }

    suspend fun listAllBundles(): List<BundleEditResult<T>?> = coroutineScope {
        val storageUri =
            settingsManager.getStoragePath() ?: return@coroutineScope emptyList()
        val storageDir = fileService.getFileFromPath(storageUri) ?: return@coroutineScope emptyList()

        val files = storageDir.listFiles()
        files.map { file ->
            async(Dispatchers.IO) {
                if (file.isDirectory()) {
                    val metaFile = file.findFile(nameProvider.jsonMetaFilename(file.name!!))
                    if (metaFile != null && metaFile.exists()) {
                        val jsonString = fileService.readText(metaFile.absolutePath())
                            ?: return@async null
                        val bookMetaInfo = deserializer.deserialize(jsonString)
                        val bookFile = file.findFile(nameProvider.previewFilename(file.name!!))
                        val previewFile =
                            file.findFile(nameProvider.fileName(file.name!!, bookMetaInfo))
                                ?: return@async null
                        val name = file.name ?: return@async null
                        val bookFileUri = bookFile?.absolutePath() ?: return@async null
                        BundleEditResult(
                            previewFile.absolutePath(),
                            name,
                            metaFile.absolutePath(),
                            bookFileUri,
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

    fun getBookFile(bundleName: String, meta: T): Result<FileWrapper> {
        // Check if the file is already in the cache
        fileCache[bundleName]?.let {
            return Result.success(FileWrapper(it))
        }

        val storageUri = settingsManager.getStoragePath()
            ?: return Result.failure(Exception("Storage directory not set"))
        val storageDir = fileService.getFileFromPath(storageUri)
            ?: return Result.failure(InvalidStorageDirectoryException())

        val bundleDir = storageDir.findFile(bundleName)
            ?: return Result.failure(Exception("Bundle not found"))

        val documentFile = bundleDir.findFile(nameProvider.fileName(bundleName, meta))
            ?: return Result.failure(Exception("File not found in storage directory"))

        val destFile = fileService.copyToLocalFile(documentFile, nameProvider.fileName(bundleName, meta))

        // Cache the file
        fileCache[bundleName] = destFile

        return Result.success(FileWrapper(destFile))
    }

    fun clearFileCache() {
        fileCache.values.forEach { it.delete() }
        fileCache.clear()
    }
}