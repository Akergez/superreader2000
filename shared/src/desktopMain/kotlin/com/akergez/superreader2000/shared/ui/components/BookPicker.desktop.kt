package com.akergez.superreader2000.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual class BookLauncher(
    private val onImageSelected: (String) -> Unit
) {
    actual fun launch() {
        val fileDialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD).apply {
            // Set file filter for images
            file = "*.pdf;*.fb2;*.djvu"
            isVisible = true
        }

        val selectedFile = fileDialog.file
        val selectedDirectory = fileDialog.directory

        if (selectedFile != null && selectedDirectory != null) {
            val file = File(selectedDirectory, selectedFile)
            if (file.exists()) {
                onImageSelected(file.toURI().toString())
            }
        }
    }
}

@Composable
actual fun rememberBookPickerLauncher(onImageSelected: (String) -> Unit): BookLauncher {
    return remember(onImageSelected) {
        BookLauncher(onImageSelected)
    }
}