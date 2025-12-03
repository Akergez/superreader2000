package com.example.bottomapp.shared.ui.platform

/**
 * Platform service interface for showing UI notifications.
 * Implementations vary by platform (Android Toast, Desktop notifications, etc.)
 */
expect interface IPlatformServices {
    fun showToast(message: String)
    fun showError(message: String)
    suspend fun pickFile(fileFilter: String = "*"): String?
}

/**
 * Platform service for file operations.
 * Implementations vary by platform.
 */
expect interface IFileService {
    fun getFileName(path: String): String
    suspend fun readFileBytes(path: String): ByteArray
    suspend fun exists(path: String): Boolean
}

