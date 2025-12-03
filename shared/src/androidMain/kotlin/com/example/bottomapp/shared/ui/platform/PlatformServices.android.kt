package com.example.bottomapp.shared.ui.platform

actual interface IPlatformServices {
    actual fun showToast(message: String)
    actual fun showError(message: String)
    actual suspend fun pickFile(fileFilter: String): String?
}

actual interface IFileService {
    actual fun getFileName(path: String): String
    actual suspend fun readFileBytes(path: String): ByteArray
    actual suspend fun exists(path: String): Boolean
}