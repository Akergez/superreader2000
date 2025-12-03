package com.example.bottomapp.ui.homescreen

import com.example.bottomapp.shared.backend.storage.BookStorageEntry


data class HoomeScreenState(
    val editingBook: BookStorageEntry? = null,
)