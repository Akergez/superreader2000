package com.example.bottomapp.shared.ui.platform

expect interface IPlatformServices {
    fun showToast(message: String)
    fun showError(message: String)
    suspend fun pickFile(fileFilter: String): String?
}

expect interface IFileService {
    fun getFileName(path: String): String
    suspend fun readFileBytes(path: String): ByteArray
    suspend fun exists(path: String): Boolean
}