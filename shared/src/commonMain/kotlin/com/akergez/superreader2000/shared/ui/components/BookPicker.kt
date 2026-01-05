package com.akergez.superreader2000.shared.ui.components

import androidx.compose.runtime.Composable

expect class BookLauncher {
    fun launch()
}

@Composable
expect fun rememberBookPickerLauncher(
    onImageSelected: (String) -> Unit
): BookLauncher
