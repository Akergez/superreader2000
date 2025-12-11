package com.example.bottomapp.shared.ui.components

import androidx.compose.runtime.Composable

expect class ImagePickerLauncher {
    fun launch()
}

@Composable
expect fun rememberImagePickerLauncher(
    onImageSelected: (String) -> Unit
): ImagePickerLauncher

