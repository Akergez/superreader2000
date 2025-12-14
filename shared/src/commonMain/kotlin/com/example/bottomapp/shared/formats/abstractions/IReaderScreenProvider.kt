package com.example.bottomapp.shared.formats.abstractions

import androidx.compose.runtime.Composable
import com.example.bottomapp.shared.backend.storage.BookStorageEntry

interface IReaderScreenProvider {
    fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit
}