package com.example.bottomapp.shared.ui.platform

import androidx.compose.runtime.Composable

/**
 * Platform-specific service for showing toast messages
 */
expect class ToastService {
    fun show(message: String, duration: ToastDuration = ToastDuration.SHORT)
}

enum class ToastDuration {
    SHORT,
    LONG
}

/**
 * Platform-specific service for showing alerts/dialogs
 */
expect class AlertService {
    @Composable
    fun showAlert(
        title: String,
        message: String,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit = {},
        confirmText: String = "OK",
        dismissText: String = "Cancel"
    )
}

/**
 * Platform-specific service for file operations
 */
expect class FileService {
    suspend fun openFilePicker(): String?
    suspend fun saveFile(fileName: String, content: ByteArray): Boolean
    suspend fun loadFile(path: String): ByteArray?
}

