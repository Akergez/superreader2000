package com.akergez.superreader2000.shared.formats.djvu

import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.IBookInfoExtractor
import java.io.File

actual class DjvuBookAuthorExtractor :
    IBookInfoExtractor<BookMetaInfo> {
    actual override fun getAuthor(file: File): String? {
        //ToDo implement
        return ""
    }
}