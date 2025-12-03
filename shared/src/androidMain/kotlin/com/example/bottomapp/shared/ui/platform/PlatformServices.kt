package com.example.bottomapp.shared.ui.platform

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable

actual class ToastService(private val context: Context) {
    actual fun show(message: String, duration: ToastDuration) {
        val duration_int = when (duration) {
            ToastDuration.SHORT -> Toast.LENGTH_SHORT
            ToastDuration.LONG -> Toast.LENGTH_LONG
        }
        Toast.makeText(context, message, duration_int).show()
    }
}

actual class AlertService {
    @Composable
    actual fun showAlert(
        title: String,
        message: String,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit,
        confirmText: String,
        dismissText: String
    ) {
        // Implementation for Android alerts would go here
        // For now, using basic Android Alert Dialog
    }
}

actual class FileService {
    actual suspend fun openFilePicker(): String? {
        // Will be implemented with platform-specific dialogs
        return null
    }

    actual suspend fun saveFile(fileName: String, content: ByteArray): Boolean {
        // Will be implemented with platform-specific file operations
        return false
    }

    actual suspend fun loadFile(path: String): ByteArray? {
        // Will be implemented with platform-specific file operations
        return null
    }
}

