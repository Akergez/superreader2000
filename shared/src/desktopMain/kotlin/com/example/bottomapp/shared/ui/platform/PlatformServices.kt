package com.example.bottomapp.shared.ui.platform

import androidx.compose.runtime.Composable
import java.io.File

actual class ToastService {
    actual fun show(message: String, duration: ToastDuration) {
        // On desktop, log to console or show in status bar
        println("Toast: $message")
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
        // Desktop alert implementation
        // Could use javax.swing.JOptionPane or custom Composable dialog
    }
}

actual class FileService {
    actual suspend fun openFilePicker(): String? {
        // Desktop file picker implementation
        // Could use JFileChooser or platform-specific dialogs
        return null
    }

    actual suspend fun saveFile(fileName: String, content: ByteArray): Boolean {
        return try {
            File(fileName).writeBytes(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual suspend fun loadFile(path: String): ByteArray? {
        return try {
            File(path).readBytes()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

