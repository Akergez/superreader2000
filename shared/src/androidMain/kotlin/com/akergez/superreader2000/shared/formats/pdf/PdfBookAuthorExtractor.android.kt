package com.akergez.superreader2000.shared.formats.pdf

import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.IBookInfoExtractor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

actual class PdfBookAuthorExtractor :
    IBookInfoExtractor<BookMetaInfo> {
    actual override fun getAuthor(file: File): String? {
        PDDocument.load(file).use { document ->
            val documentInfo = document.documentInformation
            return documentInfo.author
        }
    }
}