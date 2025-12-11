package com.example.bottomapp.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual class ImagePickerLauncher(
    private val onImageSelected: (String) -> Unit
) {

    actual fun launch() {
        val fileDialog = FileDialog(null as Frame?, "Select Image", FileDialog.LOAD).apply {
            // Set file filter for images
            file = "*.jpg;*.jpeg;*.png;*.gif;*.bmp;*.webp"
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
actual fun rememberImagePickerLauncher(
    onImageSelected: (String) -> Unit
): ImagePickerLauncher {
    return remember(onImageSelected) {
        ImagePickerLauncher(onImageSelected)
    }
}

