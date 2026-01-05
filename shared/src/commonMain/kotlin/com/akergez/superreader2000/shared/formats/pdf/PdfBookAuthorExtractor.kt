package com.akergez.superreader2000.shared.formats.pdf

import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.IBookInfoExtractor
import java.io.File

expect class PdfBookAuthorExtractor() : IBookInfoExtractor<BookMetaInfo> {
    override fun getAuthor(file: File): String?
}