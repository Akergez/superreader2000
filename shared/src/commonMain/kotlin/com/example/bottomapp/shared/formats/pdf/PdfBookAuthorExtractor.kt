package com.example.bottomapp.shared.formats.pdf

import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo
import com.example.bottomapp.shared.formats.abstractions.IBookInfoExtractor
import java.io.File

expect class PdfBookAuthorExtractor() : IBookInfoExtractor<BookMetaInfo> {
    override fun getAuthor(file: File): String?
}