package com.example.bottomapp.shared.ui.screens.homescreen

import com.example.bottomapp.shared.backend.storage.BookStorageEntry

data class HomeScreenState(
    val editingBook: BookStorageEntry? = null,
)

