package com.example.bottomapp.shared.ui.screens.homescreen

import com.example.bottomapp.shared.backend.storage.BookStorageEntry


data class HoomeScreenState(
    val editingBook: BookStorageEntry? = null,
)