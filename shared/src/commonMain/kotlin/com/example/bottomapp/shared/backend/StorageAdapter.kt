package com.example.bottomapp.shared.backend

import com.example.bottomapp.shared.backend.storage.StorageManager
import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo

expect class BookStorage : StorageManager<BookMetaInfo>