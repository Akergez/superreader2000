package com.example.bottomapp.shared.ui.components

import androidx.compose.runtime.Composable

expect class BookLauncher {
    fun launch()
}

@Composable
expect fun rememberBookPickerLauncher(
    onImageSelected: (String) -> Unit
): BookLauncher
