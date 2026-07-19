package com.akergez.superreader2000.shared.platform

import com.akergez.superreader2000.shared.formats.abstractions.FileType

expect class MessageHandler {
    fun showMessage(message: String)
    fun showLongMessage(message: String)
}

expect class FileUriHandler {
    suspend fun getFileName(fileUri: String): String
    suspend fun getFileType(fileUri: String): FileType
}
