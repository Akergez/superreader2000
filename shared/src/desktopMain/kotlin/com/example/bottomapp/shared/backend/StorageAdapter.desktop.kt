package com.example.bottomapp.shared.backend

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.storage.StorageManager

actual class BookStorage :
    StorageManager<BookMetaInfo>()