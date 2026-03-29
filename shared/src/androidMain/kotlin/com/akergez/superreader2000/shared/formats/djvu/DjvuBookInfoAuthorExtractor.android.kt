package com.akergez.superreader2000.shared.formats.djvu

import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.IBookInfoExtractor
import com.github.axet.djvulibre.DjvuLibre
import java.io.File
import java.io.FileInputStream

actual class DjvuBookAuthorExtractor :
    IBookInfoExtractor<BookMetaInfo> {
    actual override fun getAuthor(file: File): String? {
        val djvu = DjvuLibre(FileInputStream(file).fd)
        return djvu.getMeta(DjvuLibre.META_AUTHOR)
    }
}