package com.example.bottomapp.shared.formats.pdf

import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo
import com.example.bottomapp.shared.formats.abstractions.IBookInfoExtractor
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