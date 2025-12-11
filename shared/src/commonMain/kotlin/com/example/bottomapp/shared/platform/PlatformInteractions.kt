package com.example.bottomapp.shared.platform

expect class MessageHandler {
    fun showMessage(message: String)
    fun showLongMessage(message: String)
}

expect class FileUriHandler {
    suspend fun getFileName(fileUri: String): String
}