package com.example.bottomapp.shared.platform

import android.content.Context
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.net.toUri

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
}

