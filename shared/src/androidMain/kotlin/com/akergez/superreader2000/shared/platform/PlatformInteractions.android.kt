package com.akergez.superreader2000.shared.platform

import android.content.Context
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.net.toUri
import com.akergez.superreader2000.shared.formats.abstractions.FileType

actual class MessageHandler(
    private val context: Context
) {
    actual fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    actual fun showLongMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

actual class FileUriHandler(
    private val context: Context
) {
    actual suspend fun getFileName(fileUri: String): String {
        val uri = fileUri.toUri()
        var result: String? = null
        
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        
        return result ?: "unknown"
    }

    actual suspend fun getFileType(fileUri: String): FileType {
        val uri = fileUri.toUri()
        typeFromExtension(getFileName(fileUri))?.let { return it }

        when (context.contentResolver.getType(uri)?.lowercase()) {
            "application/pdf" -> return FileType.pdf
            "application/fb2", "application/x-fictionbook+xml", "text/fb2+xml" -> return FileType.fb2
            "application/djvu", "image/vnd.djvu", "image/x-djvu" -> return FileType.djvu
        }

        val header = context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            val count = input.read(buffer).coerceAtLeast(0)
            buffer.copyOf(count)
        } ?: return FileType.unknown
        if (header.size >= 5 && header.copyOfRange(0, 5).decodeToString() == "%PDF-") {
            return FileType.pdf
        }
        if (header.size >= 12 && header.copyOfRange(0, 8).decodeToString() == "AT&TFORM") {
            return FileType.djvu
        }
        val xmlStart = header.decodeToString().trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        if (xmlStart.contains("<FictionBook", ignoreCase = true)) return FileType.fb2
        return FileType.unknown
    }

    private fun typeFromExtension(name: String): FileType? =
        when (name.substringAfterLast('.', "").lowercase()) {
            "pdf" -> FileType.pdf
            "fb2" -> FileType.fb2
            "djvu", "djv" -> FileType.djvu
            else -> null
        }
}
