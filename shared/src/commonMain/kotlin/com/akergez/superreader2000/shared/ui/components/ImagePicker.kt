package com.akergez.superreader2000.shared.ui.components

import androidx.compose.runtime.Composable

expect class ImagePickerLauncher {
    fun launch()
}

@Composable
expect fun rememberImagePickerLauncher(
    onImageSelected: (String) -> Unit
): ImagePickerLauncher

