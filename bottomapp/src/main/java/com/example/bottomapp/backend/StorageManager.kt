package com.example.bottomapp.backend

import PreviewGenerator
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
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

    fun copyBookToStorage(file: Uri, meta: BookMetaInfo, contentResolver: ContentResolver, fileType: FileType): BookStorageEntry {
        val storageUri: Uri = settingsManager.getStorageUri() ?: throw Exception("Storage not set")
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: throw Exception("Invalid storage directory")
        val mimeType = fileType.toMimeType()
        val destFile = storageDir.createFile(mimeType, meta.fileName) ?: throw Exception("Failed to create destination file")

        contentResolver.openInputStream(file)?.use { input ->
            contentResolver.openOutputStream(destFile.uri)?.use { output ->
                input.copyTo(output)
            }
        }

        val metaFile = storageDir.createFile("application/json", "${meta.fileName}_meta.json") ?: throw Exception("Failed to create metadata file")

        val jsonString = gson.toJson(meta)
        contentResolver.openOutputStream(metaFile.uri)?.use { output ->
            output.write(jsonString.toByteArray())
        }

        val previewFile = storageDir.createFile("image/jpeg", "${meta.fileName}_preview.jpg") ?: throw Exception("Failed to create preview file");
        val previewBitmap = PreviewGenerator().getPreviewBitmap(getBookFile(meta.fileName)) ?: throw Exception("Failed to generate preview bitmap");
        contentResolver.openOutputStream(previewFile.uri)?.use { output ->
            previewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }

        return BookStorageEntry(meta.fileName, previewFile.uri, meta)
    }

    fun getPreviewUri(fileName: String): Uri? {
        val storageUri: Uri = settingsManager.getStorageUri() ?: throw Exception("Storage not set")
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: throw Exception("Invalid storage directory")
        val previewFile = storageDir.findFile("${fileName}_preview.jpg") ?: return null
        return previewFile.uri
    }

    fun updateProgressData(meta: BookMetaInfo) {
        val storageUri: Uri = settingsManager.getStorageUri() ?: throw Exception("Storage not set")
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: throw Exception("Invalid storage directory")
        val metaFile = storageDir.findFile("${meta.fileName}_meta.json") ?: throw Exception("Metadata file not found")

        val jsonString = gson.toJson(meta)
        context.contentResolver.openOutputStream(metaFile.uri)?.use { output ->
            output.write(jsonString.toByteArray())
        }
    }

    fun readProgressData(fileName: String): BookMetaInfo {
        val storageUri: Uri = settingsManager.getStorageUri() ?: throw Exception("Storage not set")
        val storageDir = File(storageUri.path!!)
        val metaFile = File(storageDir, "${fileName}_meta.json")

        val jsonString = metaFile.readText()
        return gson.fromJson(jsonString, BookMetaInfo::class.java)
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
                        val jsonString = context.contentResolver.openInputStream(metaFile.uri)?.bufferedReader().use { it?.readText() }
                        try {
                            val bookMetaInfo = gson.fromJson(jsonString, BookMetaInfo::class.java);
                            BookStorageEntry(bookMetaInfo.fileName, getPreviewUri(bookMetaInfo.fileName), bookMetaInfo)
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

    fun getBookFile(fileName: String): File {
        // Check if the file is already in the cache
        fileCache[fileName]?.let {
            return it
        }

        val storageUri: Uri = settingsManager.getStorageUri() ?: throw Exception("Storage not set")
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: throw Exception("Invalid storage directory")
        val documentFile = storageDir.findFile(fileName) ?: throw Exception("File not found in storage directory")

        val destFile = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(documentFile.uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        // Cache the file
        fileCache[fileName] = destFile

        return destFile
    }

    fun clearFileCache() {
        fileCache.values.forEach { it.delete() }
        fileCache.clear()
    }
}