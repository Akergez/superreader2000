package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo
import com.example.bottomapp.shared.formats.abstractions.IBookInfoExtractor
import java.io.File

expect class DjvuBookAuthorExtractor() : IBookInfoExtractor<BookMetaInfo> {
    override fun getAuthor(file: File): String?
}