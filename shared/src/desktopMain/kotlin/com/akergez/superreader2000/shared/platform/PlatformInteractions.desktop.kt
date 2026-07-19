package com.akergez.superreader2000.shared.platform

import com.akergez.superreader2000.shared.formats.abstractions.FileType

import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JWindow
import javax.swing.SwingUtilities
import javax.swing.Timer

import java.io.File
import java.net.URI

actual class MessageHandler {
    actual fun showMessage(message: String) {
        showToast(message, 2500)
    }

    actual fun showLongMessage(message: String) {
        showToast(message, 4000)
    }

    private fun showToast(message: String, durationMs: Int) {
        SwingUtilities.invokeLater {
            val label = JLabel(message).apply {
                isOpaque = true
                background = Color(45, 45, 45)
                foreground = Color.WHITE
                border = BorderFactory.createEmptyBorder(10, 16, 10, 16)
            }
            val window = JWindow().apply {
                contentPane.add(label)
                isAlwaysOnTop = true
                pack()
                setLocationRelativeTo(null)
                isVisible = true
            }
            Timer(durationMs) { window.dispose() }.apply {
                isRepeats = false
                start()
            }
        }
    }
}

actual class FileUriHandler {
    actual suspend fun getFileName(fileUri: String): String {
        val file = if (fileUri.startsWith("file:")) File(URI(fileUri)) else File(fileUri)
        return file.name
    }

    actual suspend fun getFileType(fileUri: String): FileType =
        when (getFileName(fileUri).substringAfterLast('.', "").lowercase()) {
            "pdf" -> FileType.pdf
            "fb2" -> FileType.fb2
            "djvu", "djv" -> FileType.djvu
            else -> FileType.unknown
        }
}
