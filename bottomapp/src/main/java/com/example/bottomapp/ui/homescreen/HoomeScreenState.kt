package com.example.bottomapp.ui.homescreen

import com.example.bottomapp.backend.BookStorageEntry

data class HoomeScreenState(
    val selectedBook: BookStorageEntry? = null,
    val editingBook: BookStorageEntry? = null,
)