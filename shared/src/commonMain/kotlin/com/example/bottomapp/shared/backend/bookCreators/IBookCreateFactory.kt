package com.example.bottomapp.shared.backend.bookCreators

import com.example.bottomapp.shared.backend.domain.BookMetadata
import com.example.bottomapp.shared.backend.domain.Progress

interface IBookCreateFactory {
    fun createProgress(): Progress
    fun getNameFromFileName(fileName: String): String
}
