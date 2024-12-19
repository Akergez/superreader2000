package com.example.bottomapp.ui.homescreen

import com.example.bottomapp.backend.BookStorageEntry

data class HoomeScreenState(
    val books: List<BookStorageEntry> = emptyList(),
    val selectedBook: BookStorageEntry? = null,
    val editingBook: BookStorageEntry? = null,
)