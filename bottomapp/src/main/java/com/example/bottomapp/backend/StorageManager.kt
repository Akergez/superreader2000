package com.example.bottomapp.backend

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.bottomapp.MainActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream

class StorageManager(val context: Context, val settingsManager: SettingsManager) {

    private val progressAdapterFactory = RuntimeTypeAdapterFactory
        .of(Progress::class.java, "type")
        .registerSubtype(PdfProgress::class.java, "pdf")

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(progressAdapterFactory)
        .create()

    private val fileCache = HashMap<String, File>()

    fun init(): Unit {
        if (settingsManager.getStorageUri() == null) {
            // Trigger directory picker in MainActivity
            (context as MainActivity).openDirectoryPicker()
        }
    }

    fun copyBookToStorage(file: Uri, meta: BookLink, contentResolver: ContentResolver): String {
        val storageUri: Uri = settingsManager.getStorageUri() ?: throw Exception("Storage not set")
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: throw Exception("Invalid storage directory")
        val destFile = storageDir.createFile("application/pdf", meta.fileName) ?: throw Exception("Failed to create destination file")

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

        return destFile.uri.toString()
    }

    fun updateProgressData(meta: BookLink) {
        val storageUri: Uri = settingsManager.getStorageUri() ?: throw Exception("Storage not set")
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: throw Exception("Invalid storage directory")
        val metaFile = storageDir.findFile("${meta.fileName}_meta.json") ?: throw Exception("Metadata file not found")

        val jsonString = gson.toJson(meta)
        context.contentResolver.openOutputStream(metaFile.uri)?.use { output ->
            output.write(jsonString.toByteArray())
        }
    }

    fun readProgressData(fileName: String): BookLink {
        val storageUri: Uri = settingsManager.getStorageUri() ?: throw Exception("Storage not set")
        val storageDir = File(storageUri.path!!)
        val metaFile = File(storageDir, "${fileName}_meta.json")

        val jsonString = metaFile.readText()
        return gson.fromJson(jsonString, BookLink::class.java)
    }

    fun listAllBooks(): List<BookLink> {
        val storageUri: Uri = settingsManager.getStorageUri() ?: return emptyList()
        val storageDir = DocumentFile.fromTreeUri(context, storageUri) ?: return emptyList()

        return storageDir.listFiles().mapNotNull { file ->
            val metaFile = storageDir.findFile("${file.name}_meta.json")
            if (metaFile != null && metaFile.exists()) {
                val jsonString = context.contentResolver.openInputStream(metaFile.uri)?.bufferedReader().use { it?.readText() }
                gson.fromJson(jsonString, BookLink::class.java)
            } else {
                null
            }
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