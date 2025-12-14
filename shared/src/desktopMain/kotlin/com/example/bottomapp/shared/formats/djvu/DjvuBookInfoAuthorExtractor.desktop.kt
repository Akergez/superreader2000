package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo
import com.example.bottomapp.shared.formats.abstractions.IBookInfoExtractor
import java.io.File

actual class DjvuBookAuthorExtractor :
    IBookInfoExtractor<BookMetaInfo> {
    actual override fun getAuthor(file: File): String? {
        //ToDo implement
        return ""
    }
}