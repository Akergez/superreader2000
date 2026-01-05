package com.akergez.superreader2000.shared.formats.djvu

import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.IBookInfoExtractor
import java.io.File

expect class DjvuBookAuthorExtractor() : IBookInfoExtractor<BookMetaInfo> {
    override fun getAuthor(file: File): String?
}