package com.akergez.superreader2000.shared.backend.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.akergez.superreader2000.shared.backend.exceptions.FailedToCreateFileException
import com.akergez.superreader2000.shared.backend.exceptions.InvalidStorageDirectoryException
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.formats.abstractions.IBookInfoExtractorFactory
import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.akergez.superreader2000.shared.backend.previewGenerators.Image
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream

actual class FileService(val context: Context) {
    actual fun getFileFromPath(path: String): DocumentFileWrapper? {
        val documentFile = DocumentFile.fromTreeUri(context, path.toUri()) ?: return null
        return DocumentFileWrapper(documentFile)
    }

    actual fun copy(source: String, destination: String) {
        context.contentResolver.openInputStream(source.toUri())?.use { inputStream ->
            context.contentResolver.openOutputStream(destination.toUri())?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    actual fun writeText(path: String, content: String) {
        context.contentResolver.openOutputStream(path.toUri())?.use { output ->
            output.write(content.toByteArray())
        }
    }

    actual fun writeBitmap(path: String, bitmap: Image) {
        context.contentResolver.openOutputStream(path.toUri())?.use { output ->
            bitmap.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
    }

    actual fun readText(path: String): String? {
        return context.contentResolver.openInputStream(path.toUri())?.bufferedReader()?.use { it.readText() }
    }

    actual fun copyToLocalFile(source: DocumentFileWrapper, destFileName: String): File {
        val destFile = File(context.filesDir, destFileName)
        context.contentResolver.openInputStream(source.uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    actual fun readBytes(path: String, offset: Long, length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        val descriptor = context.contentResolver.openFileDescriptor(path.toUri(), "r") ?: return ByteArray(0)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).channel.use { channel ->
                channel.position(offset.coerceAtLeast(0))
                val buffer = java.nio.ByteBuffer.allocate(length)
                while (buffer.hasRemaining() && channel.read(buffer) > 0) Unit
                buffer.flip()
                ByteArray(buffer.remaining()).also(buffer::get)
            }
        }
    }

    actual fun writeBytes(path: String, bytes: ByteArray) {
        context.contentResolver.openOutputStream(path.toUri(), "wt")?.use { it.write(bytes) }
            ?: error("cannot open $path for writing")
    }
}

actual class ContentResolverWrapper(val ContentResolver: ContentResolver)
actual class DocumentFileWrapper(val file: DocumentFile) {
    val uri: Uri get() = file.uri
    actual val name: String? get() = file.name

    actual fun exists(): Boolean = file.exists()

    actual fun findFile(name: String): DocumentFileWrapper? {
        return DocumentFileWrapper(
            file.findFile(name) ?: return null
        )
    }

    actual fun absolutePath(): String {
        return file.uri.toString()
    }

    actual fun createDirectory(name: String): DocumentFileWrapper? {
        val newDir = file.createDirectory(name) ?: return null
        return DocumentFileWrapper(newDir)
    }

    actual fun createFile(mimeType: String, name: String): DocumentFileWrapper? {
        val file = file.createFile(mimeType, name) ?: return null
        return DocumentFileWrapper(file)
    }

    actual fun renameTo(name: String): Boolean {
       return file.renameTo(name)
    }

    actual fun delete(): Boolean {
        file.delete()
        return true
    }

    actual fun listFiles(): List<DocumentFileWrapper> {
       return file.listFiles().map { DocumentFileWrapper(it) }
    }

    actual fun isDirectory(): Boolean {
        return file.isDirectory
    }
    actual fun length(): Long = file.length()
    actual fun lastModified(): Long = file.lastModified()
}
