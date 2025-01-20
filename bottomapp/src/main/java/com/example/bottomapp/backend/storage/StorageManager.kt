package com.example.bottomapp.backend.storage

import com.example.bottomapp.backend.PreviewGenerator
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.bottomapp.IBookInfoExtractorFactory
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.exceptions.FailedToCreateFileException
import com.example.bottomapp.backend.exceptions.InvalidStorageDirectoryException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class BundleCreateResult<TMeta>(
    val bookFile: Uri,
    val bundleName: String,
    val metaFileUri: Uri,
    val previewUri: Uri,
    val meta: TMeta,
    val author: String?
)

data class BundleEditResult<TMeta>(
    val bookFile: Uri,
    val bundleName: String,
    val metaFileUri: Uri,
    val previewUri: Uri,
    val meta: TMeta,
)

abstract class StorageManager<T>(
    val context: Context,
    private val settingsManager: SettingsManager,
    private val deserializer: IDeserializer<T>,
    private val nameProvider: INameProvider<T>,
    private val infoExtractor: IBookInfoExtractorFactory<T>,
) {
    private val fileCache = HashMap<String, File>()

    suspend fun updateBundlePreview(newBundlePreview: Uri, bundleName: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            Log.i("StorageManager", "Updating preview for $bundleName")
            val storageUri = settingsManager.getStorageUri()
                ?: return@withContext Result.failure(Exception("Storage directory not set"))
            val storageDir = DocumentFile.fromTreeUri(context, storageUri)
                ?: return@withContext Result.failure(Exception("Invalid storage directory"))
            val bundleDir = storageDir.findFile(bundleName)
                ?: return@withContext Result.failure(Exception("Bundle not found"))
            val fileName = nameProvider.previewFilename(bundleName)

            val existingFile = bundleDir.findFile(fileName)
                ?: bundleDir.createFile("image/jpeg", fileName)
                ?: return@withContext Result.failure(Exception("Failed to create preview file"))

            context.contentResolver.openInputStream(newBundlePreview)?.use { inputStream ->
                context.contentResolver.openOutputStream(existingFile.uri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return@withContext Result.success(existingFile.uri)
        }

    suspend fun createBundle(
        bundleName: String,
        file: Uri,
        meta: T,
        contentResolver: ContentResolver,
        mimeType: String
    ): Result<BundleCreateResult<T>> = withContext(Dispatchers.IO) {
        Log.i("StorageManager", "Creating bundle $bundleName")
        val storageUri = settingsManager.getStorageUri()
            ?: return@withContext Result.failure(Exception("Storage directory not set"))
        val storageDir = DocumentFile.fromTreeUri(context, storageUri)
            ?: return@withContext Result.failure(Exception("Invalid storage directory"))

        val existingFile = storageDir.findFile(bundleName)
        if (existingFile != null) {
            return@withContext Result.failure(Exception("Bundle already exists"))
        }

        val bundleStorage = storageDir.createDirectory(bundleName)
            ?: return@withContext Result.failure(Exception("Failed to create bundle directory"))

        val destFile = bundleStorage.createFile(mimeType, nameProvider.fileName(bundleName, meta))
            ?: throw Exception("Failed to create destination file")

        contentResolver.openInputStream(file)?.use { input ->
            contentResolver.openOutputStream(destFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }

        val metaFile =
            bundleStorage.createFile("application/json", nameProvider.jsonMetaFilename(bundleName))
                ?: return@withContext Result.failure(
                    FailedToCreateFileException()
                )

        val jsonString = deserializer.serialize(meta)
        contentResolver.openOutputStream(metaFile.uri)?.use { output ->
            output.write(jsonString.toByteArray())
        }

        val previewFile =
            bundleStorage.createFile("image/jpeg", nameProvider.previewFilename(bundleName))
                ?: return@withContext Result.failure(
                    Exception("Failed to create preview file")
                )
        var bookFile: File? = null
        getBookFile(bundleName, meta).onSuccess { bookFile = it }
            .onFailure { return@withContext Result.failure(it) }

        val previewBitmap =
            PreviewGenerator().getPreviewBitmap(bookFile!!) ?: return@withContext Result.failure(
                Exception("Failed to generate preview bitmap")
            )
        contentResolver.openOutputStream(previewFile.uri)?.use { output ->
            previewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
        return@withContext Result.success(
            BundleCreateResult<T>(
                destFile.uri,
                bundleName,
                metaFile.uri,
                previewFile.uri,
                meta,
                infoExtractor.createExtractor(meta).getAuthor(bookFile!!)
            )
        )
    }

    suspend fun renameBundle(
        old: String,
        new: String,
        meta: T
    ): Result<BundleEditResult<T>> = withContext(Dispatchers.IO) {
        Log.i("StorageManager", "Renaming bundle $old to $new")
        val storageUri = settingsManager.getStorageUri()
            ?: return@withContext Result.failure(Exception("Storage directory not set"))
        val storageDir = DocumentFile.fromTreeUri(context, storageUri)
            ?: return@withContext Result.failure(Exception("Invalid storage directory"))

        val bundleDir = storageDir.findFile(old)
            ?: return@withContext Result.failure(Exception("Bundle not found"))

        bundleDir.renameTo(new)

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
                oldFilePath.uri,
                new,
                oldJsonPath.uri,
                oldImagePath.uri,
                meta,
            )
        )
    }

    suspend fun deleteBundle(bundleName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            Log.i("StorageManager", "Deleting bundle $bundleName")
            val storageUri = settingsManager.getStorageUri()
                ?: return@withContext Result.failure(Exception("Storage directory not set"))
            val storageDir =
                DocumentFile.fromTreeUri(context, storageUri) ?: return@withContext Result.failure(
                    InvalidStorageDirectoryException()
                )

            val bundleDir = storageDir.findFile(bundleName) ?: return@withContext Result.failure(
                Exception("Bundle not found")
            )

            bundleDir.delete()

            return@withContext Result.success(Unit)
        }

    private fun getPreviewUri(bundleName: String): Result<Uri?> {
        Log.i("StorageManager", "Getting preview for $bundleName")
        val storageUri = settingsManager.getStorageUri()
            ?: return Result.failure(Exception("Storage directory not set"))
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: return Result.failure(
            InvalidStorageDirectoryException()
        )
        val bundleDir =
            storageDir.findFile(bundleName) ?: return Result.failure(Exception("Bundle not found"))

        val previewFile =
            bundleDir.findFile(nameProvider.previewFilename(bundleName)) ?: return Result.failure(
                Exception("Preview file not found")
            )
        return Result.success(previewFile.uri)
    }

    suspend fun updateBundleMeta(bundleName: String, meta: T): Result<Unit> =
        withContext(Dispatchers.IO) {
            Log.i("StorageManager", "Updating metadata for $bundleName")
            val storageUri = settingsManager.getStorageUri()
                ?: return@withContext Result.failure(Exception("Storage directory not set"))
            val storageDir =
                DocumentFile.fromTreeUri(context, storageUri) ?: return@withContext Result.failure(
                    InvalidStorageDirectoryException()
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
            context.contentResolver.openOutputStream(updatedMetaFile.uri)?.use { output ->
                output.write(jsonString.toByteArray())
            }
            return@withContext Result.success(Unit)
        }

    suspend fun listAllBundles(): List<BundleEditResult<T>?> = coroutineScope {
        Log.i("StorageManager", "Listing all bundles")
        val storageUri = settingsManager.getStorageUri() ?: return@coroutineScope emptyList()
        val storageDir =
            DocumentFile.fromTreeUri(context, storageUri) ?: return@coroutineScope emptyList()

        val files = storageDir.listFiles()
        files.map { file ->
            async(Dispatchers.IO) {
                if (file.isDirectory) {
                    val metaFile = file.findFile(nameProvider.jsonMetaFilename(file.name!!))
                    if (metaFile != null && metaFile.exists()) {
                        val jsonString =
                            context.contentResolver.openInputStream(metaFile.uri)?.bufferedReader()
                                .use { it?.readText() }
                        val bookMetaInfo = deserializer.deserialize(jsonString!!)
                        val bookFile = file.findFile(nameProvider.previewFilename(file.name!!))
                        val previewFile =
                            file.findFile(nameProvider.fileName(file.name!!, bookMetaInfo))
                        BundleEditResult(
                            previewFile?.uri!!,
                            file.name!!,
                            metaFile.uri,
                            bookFile?.uri!!,
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

    fun getBookFile(bundleName: String, meta: T): Result<File> {
        Log.i("StorageManager", "Getting book file for $bundleName")
        // Check if the file is already in the cache
        val storageUri = settingsManager.getStorageUri()
            ?: return Result.failure(Exception("Storage directory not set"))
        fileCache[bundleName]?.let {
            return Result.success(it)
        }

        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: return Result.failure(
            InvalidStorageDirectoryException()
        )
        val bundleDir = storageDir.findFile(bundleName) ?: return Result.failure(
            Exception("Bundle not found")
        )
        val documentFile = bundleDir.findFile(nameProvider.fileName(bundleName, meta))
            ?: return Result.failure(Exception("File not found in storage directory"))

        val destFile = File(context.filesDir, nameProvider.fileName(bundleName, meta))
        context.contentResolver.openInputStream(documentFile.uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        // Cache the file
        fileCache[bundleName] = destFile

        return Result.success(destFile)
    }

    fun clearFileCache() {
        fileCache.values.forEach { it.delete() }
        fileCache.clear()
    }
}