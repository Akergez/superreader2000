package com.akergez.superreader2000.shared.formats.abstractions

import androidx.compose.runtime.Composable
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry

interface IReaderScreenProvider {
    fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit
}