package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo
import com.example.bottomapp.shared.formats.abstractions.IBookInfoExtractor
import com.github.axet.djvulibre.DjvuLibre
import java.io.File
import java.io.FileInputStream

actual class DjvuBookAuthorExtractor :
    IBookInfoExtractor<BookMetaInfo> {
    actual override fun getAuthor(file: File): String? {
        val djvu = DjvuLibre(FileInputStream(file).fd)
        return djvu.author
    }
}