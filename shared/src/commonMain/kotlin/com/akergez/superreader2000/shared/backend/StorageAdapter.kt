package com.akergez.superreader2000.shared.backend

import com.akergez.superreader2000.shared.backend.storage.StorageManager
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo

expect class BookStorage : StorageManager<BookMetaInfo>