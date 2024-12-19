package com.example.bottomapp.backend.storage

import PreviewGenerator
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.exceptions.FailedToCreateFileException
import com.example.bottomapp.backend.exceptions.InvalidStorageDirectoryException
import java.io.File
import java.io.FileOutputStream

data class BundleDataName<TMeta>(
    val bookFile: Uri,
    val bundleName: String,
    val metaFileUri: Uri,
    val previewUri: Uri,
    val meta: TMeta
)

open class StorageManager<T>(
    val context: Context,
    private val settingsManager: SettingsManager,
    private val deserializer: IDeserializer<T>,
    private val nameProvider: INameProvider<T>
) {
    private val fileCache = HashMap<String, File>()

    fun updateBundlePreview(newBundlePreview: Uri, bundleName: String): Result<Uri> {
        val storageUri = settingsManager.getStorageUri()
            ?: return Result.failure(Exception("Storage directory not set"))
        val storageDir = DocumentFile.fromTreeUri(context, storageUri)
            ?: return Result.failure(Exception("Invalid storage directory"))
        val bundleDir = storageDir.findFile(bundleName)
            ?: return Result.failure(Exception("Bundle not found"))
        val fileName = nameProvider.previewFilename(bundleName)

        val existingFile = bundleDir.findFile(fileName)
            ?: bundleDir.createFile("image/jpeg", fileName)
            ?: return Result.failure(Exception("Failed to create preview file"))

        context.contentResolver.openInputStream(newBundlePreview)?.use { inputStream ->
            context.contentResolver.openOutputStream(existingFile.uri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return Result.success(existingFile.uri)
    }

    fun createBundle(
        bundleName: String,
        file: Uri,
        meta: T,
        contentResolver: ContentResolver,
        mimeType: String
    ): Result<BundleDataName<T>> {
        val storageUri = settingsManager.getStorageUri()
            ?: return Result.failure(Exception("Storage directory not set"))
        val storageDir = DocumentFile.fromTreeUri(context, storageUri)
            ?: return Result.failure(Exception("Invalid storage directory"))

        val existingFile = storageDir.findFile(bundleName)
        if (existingFile != null) {
            return Result.failure(Exception("Bundle already exists"))
        }

        val bundleStorage = storageDir.createDirectory(bundleName)
            ?: return Result.failure(Exception("Failed to create bundle directory"))

        val destFile = bundleStorage.createFile(mimeType, nameProvider.fileName(bundleName, meta))
            ?: throw Exception("Failed to create destination file")

        contentResolver.openInputStream(file)?.use { input ->
            contentResolver.openOutputStream(destFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }

        val metaFile =
            bundleStorage.createFile("application/json", nameProvider.jsonMetaFilename(bundleName))
                ?: return Result.failure(
                    FailedToCreateFileException()
                )

        val jsonString = deserializer.serialize(meta)
        contentResolver.openOutputStream(metaFile.uri)?.use { output ->
            output.write(jsonString.toByteArray())
        }

        val previewFile =
            bundleStorage.createFile("image/jpeg", nameProvider.previewFilename(bundleName))
                ?: return Result.failure(
                    Exception("Failed to create preview file")
                )
        var bookFile: File? = null
        getBookFile(bundleName, meta).onSuccess { bookFile = it }
            .onFailure { return Result.failure(it) }

        val previewBitmap =
            PreviewGenerator().getPreviewBitmap(bookFile!!) ?: return Result.failure(
                Exception("Failed to generate preview bitmap")
            )
        contentResolver.openOutputStream(previewFile.uri)?.use { output ->
            previewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
        return Result.success(
            BundleDataName<T>(
                destFile.uri,
                bundleName,
                metaFile.uri,
                previewFile.uri,
                meta
            )
        )
    }

    fun renameBundle(
        old: String,
        new: String,
        meta: T
    ): Result<BundleDataName<T>> {
        val storageUri = settingsManager.getStorageUri()
            ?: return Result.failure(Exception("Storage directory not set"))
        val storageDir = DocumentFile.fromTreeUri(context, storageUri)
            ?: return Result.failure(Exception("Invalid storage directory"))

        val bundleDir = storageDir.findFile(old)
            ?: return Result.failure(Exception("Bundle not found"))

        bundleDir.renameTo(new)

        val oldJsonPath = bundleDir.findFile(nameProvider.jsonMetaFilename(old))
            ?: return Result.failure(Exception("Metadata file not found"))
        val oldImagePath = bundleDir.findFile(nameProvider.previewFilename(old))
            ?: return Result.failure(Exception("Preview file not found"))
        val oldFilePath = bundleDir.findFile(nameProvider.fileName(old, meta))
            ?: return Result.failure(Exception("Book file not found"))

        oldJsonPath.renameTo(nameProvider.jsonMetaFilename(new))
        oldImagePath.renameTo(nameProvider.previewFilename(new))
        oldFilePath.renameTo(nameProvider.fileName(new, meta))

        return Result.success(
            BundleDataName(
                oldFilePath.uri,
                new,
                oldJsonPath.uri,
                oldImagePath.uri,
                meta
            )
        )
    }

    private fun getPreviewUri(bundleName: String): Result<Uri?> {
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

    fun updateBundleMeta(bundleName: String, meta: T): Result<Unit> {
        val storageUri = settingsManager.getStorageUri()
            ?: return Result.failure(Exception("Storage directory not set"))
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: return Result.failure(
            InvalidStorageDirectoryException()
        )

        val bundleDir = storageDir.findFile(bundleName) ?: return Result.failure(
            Exception("Bundle not found")
        )

        val metaFile = bundleDir.findFile(nameProvider.jsonMetaFilename(bundleName))
            ?: return Result.failure(
                Exception("Metadata file not found")
            )
        metaFile.delete()
        val updatedMetaFile = bundleDir.createFile("application/json", nameProvider.jsonMetaFilename(bundleName))
            ?: return Result.failure(
                Exception("Failed to create metadata file")
            )

        val jsonString = deserializer.serialize(meta)
        context.contentResolver.openOutputStream(updatedMetaFile.uri)?.use { output ->
            output.write(jsonString.toByteArray())
        }
        return Result.success(Unit)
    }

    fun listAllBundles(): List<BundleDataName<T>?> {
        val storageUri = settingsManager.getStorageUri() ?: return emptyList()
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: return emptyList()

        val files = storageDir.listFiles()
        return files.map<DocumentFile, BundleDataName<T>?> {
            if (it.isDirectory) {
                val metaFile = it.findFile(nameProvider.jsonMetaFilename(it.name!!))
                if (metaFile != null && metaFile.exists()) {
                    val jsonString =
                        context.contentResolver.openInputStream(metaFile.uri)?.bufferedReader()
                            .use { it?.readText() }
                    val bookMetaInfo = deserializer.deserialize(jsonString!!)
                    val bookFile = it.findFile(nameProvider.previewFilename(it.name!!))
                    val previewFile = it.findFile(nameProvider.fileName(it.name!!, bookMetaInfo))
                    BundleDataName(
                        previewFile?.uri!!,
                        it.name!!,
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
    }

    fun getBookFile(bundleName: String, meta: T): Result<File> {
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

        val destFile = File(context.filesDir, bundleName)
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