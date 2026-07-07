package com.akergez.superreader2000.shared.backend.storage

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.backend.previewGenerators.Image
import java.io.File
import java.net.URI
import javax.imageio.ImageIO

actual class FileService() {
    private fun uriOrPathToFile(path: String): File =
        if (path.startsWith("file:")) File(URI(path)) else File(path)

    actual fun getFileFromPath(path: String): DocumentFileWrapper? {
        val file = uriOrPathToFile(path)
        if (!file.exists()) return null
        return DocumentFileWrapper(file)
    }

    actual fun copy(source: String, destination: String) {
        uriOrPathToFile(source).copyTo(File(destination), overwrite = true)
    }

    actual fun writeText(path: String, content: String) {
        File(path).writeText(content)
    }

    actual fun readText(path: String): String? {
        val file = File(path)
        if (!file.exists()) return null
        return file.readText()
    }

    actual fun writeBitmap(path: String, bitmap: Image) {
        ImageIO.write(bitmap.bufferedImage, "jpeg", File(path))
    }

    actual fun copyToLocalFile(source: DocumentFileWrapper, destFileName: String): File {
        // On desktop, the file is already local, so just return it directly
        return source.file
    }

    actual fun readBytes(path: String, offset: Long, length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        return java.io.RandomAccessFile(uriOrPathToFile(path), "r").use { file ->
            file.seek(offset.coerceAtLeast(0))
            val bytes = ByteArray(length)
            val read = file.read(bytes)
            if (read <= 0) ByteArray(0) else bytes.copyOf(read)
        }
    }

    actual fun writeBytes(path: String, bytes: ByteArray) {
        uriOrPathToFile(path).apply { parentFile?.mkdirs() }.writeBytes(bytes)
    }
}

actual class ContentResolverWrapper

actual class DocumentFileWrapper(val file: File) {
    actual val name: String? get() = file.name

    actual fun findFile(name: String): DocumentFileWrapper? {
        if (!file.exists() || !file.isDirectory) {
            return null
        }

        val newFile = File(file.path, name)

        if (!newFile.exists()) return null

        return DocumentFileWrapper(newFile)
    }

    actual fun absolutePath(): String {
        return file.absolutePath
    }

    actual fun createDirectory(name: String): DocumentFileWrapper? {
        val newFile = File(file.absolutePath, name)
        if (!newFile.mkdirs()) return null
        return DocumentFileWrapper(newFile)
    }

    actual fun createFile(mimeType: String, name: String): DocumentFileWrapper? {
        return DocumentFileWrapper(File(file.absolutePath, name))
    }

    actual fun renameTo(name: String): Boolean {
        return file.renameTo(File(file.parentFile, name))
    }

    actual fun delete(): Boolean {
        file.deleteRecursively()
        return true
    }

    actual fun listFiles(): List<DocumentFileWrapper> {
        return file.listFiles()?.map { DocumentFileWrapper(it) } ?: emptyList()
    }

    actual fun isDirectory(): Boolean {
        return file.isDirectory
    }

    actual fun exists(): Boolean {
        return file.exists()
    }
    actual fun length(): Long = file.length()
    actual fun lastModified(): Long = file.lastModified()
}
