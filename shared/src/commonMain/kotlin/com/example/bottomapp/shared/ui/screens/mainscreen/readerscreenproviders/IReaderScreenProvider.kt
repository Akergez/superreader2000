package com.example.bottomapp.shared.ui.screens.mainscreen.readerscreenproviders

import androidx.compose.runtime.Composable
import com.example.bottomapp.shared.backend.storage.BookStorageEntry

interface IReaderScreenProvider {
    fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit
}