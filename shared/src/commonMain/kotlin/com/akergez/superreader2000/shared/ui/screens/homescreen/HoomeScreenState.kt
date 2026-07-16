package com.akergez.superreader2000.shared.ui.screens.homescreen

import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry


data class HoomeScreenState(
    val editingBook: BookStorageEntry? = null,
    val isSyncConfigured: Boolean = false,
    val isSyncRunning: Boolean = false,
)
