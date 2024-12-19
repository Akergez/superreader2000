package com.example.bottomapp.backend

import PreviewGenerator
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.bottomapp.backend.renderHelper.NameGenerator
import com.example.bottomapp.backend.exceptions.FailedToCreateFileException
import com.example.bottomapp.backend.exceptions.InvalidStorageDirectoryException
import com.example.bottomapp.backend.exceptions.StorageNotSetException
import com.example.bottomapp.constants.FileType
import com.example.bottomapp.constants.toMimeType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class StorageManager(val context: Context, val settingsManager: SettingsManager) {

    private val progressAdapterFactory = RuntimeTypeAdapterFactory
        .of(Progress::class.java, "type")
        .registerSubtype(PdfProgress::class.java, "pdf")
        .registerSubtype(Fb2Progress::class.java, "fb2")

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(progressAdapterFactory)
        .create()

    private val fileCache = HashMap<String, File>()

    private fun createJsonMetaFile(
        storageDir: DocumentFile,
        meta: BookMetaInfo,
        contentResolver: ContentResolver,
        fileName: String
    ): Result<Unit> {
        val metaFile = storageDir.createFile("application/json", fileName) ?: return Result.failure(
            FailedToCreateFileException()
        )

        val jsonString = gson.toJson(meta)
        contentResolver.openOutputStream(metaFile.uri)?.use { output ->
            output.write(jsonString.toByteArray())
        }
        return Result.success(Unit)
    }

    private fun createPreviewFile(
        storageDir: DocumentFile,
        meta: BookMetaInfo,
        contentResolver: ContentResolver,
        fileName: String
    ): Result<DocumentFile> {
        val previewFile = storageDir.createFile("image/jpeg", fileName) ?: return Result.failure(
            Exception("Failed to create preview file")
        )
        lateinit var bookFile: File
        getBookFile(meta.fileName).onSuccess { bookFile = it }
            .onFailure { return Result.failure(it) }
        val previewBitmap = PreviewGenerator().getPreviewBitmap(bookFile) ?: return Result.failure(
            Exception("Failed to generate preview bitmap")
        )
        contentResolver.openOutputStream(previewFile.uri)?.use { output ->
            previewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
        return Result.success(previewFile)
    }

    private fun createNewBook(
        storageDir: DocumentFile,
        file: Uri,
        mimeType: String,
        meta: BookMetaInfo,
        contentResolver: ContentResolver
    ): Result<BookStorageEntry> {
        val destFile = storageDir.createFile(mimeType, meta.fileName)
            ?: throw Exception("Failed to create destination file")

        contentResolver.openInputStream(file)?.use { input ->
            contentResolver.openOutputStream(destFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }

        createJsonMetaFile(storageDir, meta, contentResolver, NameGenerator.jsonMetaFilename(meta.fileName))
        val previewFile = createPreviewFile(
            storageDir,
            meta,
            contentResolver,
            NameGenerator.previewFilename(meta.fileName)
        )

        return when {
            previewFile.isSuccess -> Result.success(BookStorageEntry(meta.fileName, previewFile.getOrNull()!!.uri, meta))
            else -> Result.failure(previewFile.exceptionOrNull()!!)
        }
    }

    fun copyBookToStorage(
        file: Uri,
        meta: BookMetaInfo,
        contentResolver: ContentResolver,
        fileType: FileType
    ): Result<BookStorageEntry> {
        val storageUri: Uri = settingsManager.getStorageUri() ?: throw Exception("Storage not set")
        val storageDir = DocumentFile.fromTreeUri(context, storageUri)
            ?: throw Exception("Invalid storage directory")
        val mimeType = fileType.toMimeType()

        val existingFile = storageDir.findFile(meta.fileName)
        if (existingFile != null) {
            val jsonMetaFilename = NameGenerator.jsonMetaFilename(meta.fileName)
            val existingJsonFile = storageDir.findFile(jsonMetaFilename)
            if (existingJsonFile == null) {
                createJsonMetaFile(storageDir, meta, contentResolver, jsonMetaFilename)
                val previewFile = createPreviewFile(
                    storageDir,
                    meta,
                    contentResolver,
                    NameGenerator.previewFilename(meta.fileName)
                )

                return when {
                    previewFile.isSuccess -> Result.success(BookStorageEntry(meta.fileName, previewFile.getOrNull()!!.uri, meta))
                    else -> Result.failure(previewFile.exceptionOrNull()!!)
                }
            }
            return Result.failure(Exception("Book already exists"))
        } else {
            return createNewBook(storageDir, file, mimeType, meta, contentResolver)
        }
    }

    fun renameBook(old: BookStorageEntry,
                   new: BookStorageEntry,
                   contentResolver: ContentResolver,
                   ): Result<BookStorageEntry> {
        val storageUri: Uri = settingsManager.getStorageUri()
            ?: return Result.failure(Exception("Invalid storage directory"))

        val storageDir = DocumentFile.fromTreeUri(context, storageUri)
            ?:  return Result.failure(Exception("Invalid storage directory"))

        val oldJsonPath = NameGenerator.jsonMetaFilename(old.bookMetaInfo.fileName)
        val newImagePath = NameGenerator.previewFilename(new.bookMetaInfo.fileName)
        val oldImagePath = NameGenerator.previewFilename(old.bookMetaInfo.fileName)

        storageDir.findFile(oldImagePath)
        storageDir.findFile(oldJsonPath)?.delete()

        val oldImage = storageDir.findFile(oldImagePath)
        val oldBook = storageDir.findFile(old.bookMetaInfo.fileName)

        var success = oldImage?.renameTo(newImagePath)
        success = oldBook?.renameTo(new.bookMetaInfo.fileName)


        val newImage = storageDir.findFile(newImagePath)
        val newBook = storageDir.findFile(new.bookMetaInfo.fileName)

        val updatedBook = new.copy(
            fileName = newBook.toString(),
            previewUri = newImage?.uri,
        )
        createJsonMetaFile(
            storageDir,
            updatedBook.bookMetaInfo,
            contentResolver,
            NameGenerator.jsonMetaFilename(new.bookMetaInfo.fileName)
        )

        return Result.success(updatedBook)
    }

    private fun getPreviewUri(fileName: String): Result<Uri?> {
        val storageUri: Uri =
            settingsManager.getStorageUri() ?: return Result.failure(StorageNotSetException())
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: return Result.failure(
            InvalidStorageDirectoryException()
        )
        val previewFile =
            storageDir.findFile("${fileName}_preview.jpg") ?: return Result.success(null)
        return Result.success(previewFile.uri)
    }

    fun updateProgressData(meta: BookMetaInfo): Result<Unit> {
        val storageUri: Uri =
            settingsManager.getStorageUri() ?: return Result.failure(StorageNotSetException())
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: return Result.failure(
            InvalidStorageDirectoryException()
        )
        val metaFile = storageDir.findFile("${meta.fileName}_meta.json") ?: return Result.failure(
            Exception("Metadata file not found")
        )

        val jsonString = gson.toJson(meta)
        context.contentResolver.openOutputStream(metaFile.uri)?.use { output ->
            output.write(jsonString.toByteArray())
        }
        return Result.success(Unit)
    }

    suspend fun listAllBooks(): List<BookStorageEntry> {
        val storageUri: Uri = settingsManager.getStorageUri() ?: return emptyList()
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: return emptyList()

        return withContext(Dispatchers.IO) {
            val files = storageDir.listFiles()
            val bookMetaInfos = files.mapNotNull { file ->
                async(Dispatchers.IO) {
                    val metaFile = storageDir.findFile("${file.name}_meta.json")
                    if (file.name?.get(0) != '.' && metaFile != null && metaFile.exists()) {
                        val jsonString =
                            context.contentResolver.openInputStream(metaFile.uri)?.bufferedReader()
                                .use { it?.readText() }
                        try {
                            val bookMetaInfo = gson.fromJson(jsonString, BookMetaInfo::class.java)
                            BookStorageEntry(
                                bookMetaInfo.fileName,
                                getPreviewUri(bookMetaInfo.fileName).getOrNull(),
                                bookMetaInfo
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
            bookMetaInfos
        }
    }

    fun getBookFile(fileName: String): Result<File> {
        // Check if the file is already in the cache
        fileCache[fileName]?.let {
            return Result.success(it)
        }

        val storageUri: Uri =
            settingsManager.getStorageUri() ?: return Result.failure(StorageNotSetException())
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: return Result.failure(
            InvalidStorageDirectoryException()
        )
        val documentFile = storageDir.findFile(fileName)
            ?: return Result.failure(Exception("File not found in storage directory"))

        val destFile = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(documentFile.uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        // Cache the file
        fileCache[fileName] = destFile

        return Result.success(destFile)
    }

    fun clearFileCache() {
        fileCache.values.forEach { it.delete() }
        fileCache.clear()
    }
}