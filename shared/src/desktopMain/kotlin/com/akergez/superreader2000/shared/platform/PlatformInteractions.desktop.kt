package com.akergez.superreader2000.shared.platform

import java.io.File

actual class MessageHandler {
    actual fun showMessage(message: String) {
        println("INFO: $message")
    }

    actual fun showLongMessage(message: String) {
        println("ERROR: $message")
    }
}

actual class FileUriHandler {
    actual suspend fun getFileName(fileUri: String): String {
        val file = File(fileUri)
        return file.name
    }
}

