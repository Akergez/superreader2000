package com.akergez.superreader2000.shared.sync

import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry

data class SyncResult(
    val books: List<BookStorageEntry>,
    val pulled: Int,
    val pushed: Int,
)
