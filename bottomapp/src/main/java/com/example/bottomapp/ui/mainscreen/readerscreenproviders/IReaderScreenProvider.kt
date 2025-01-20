package com.example.bottomapp.ui.mainscreen.readerscreenproviders

import androidx.compose.runtime.Composable
import com.example.bottomapp.backend.BookStorageEntry
import com.example.bottomapp.backend.Progress

interface IReaderScreenProvider {
    fun getReaderScreen(bookLink: BookStorageEntry): @Composable () -> Unit
}