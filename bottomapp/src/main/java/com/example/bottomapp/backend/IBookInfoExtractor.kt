package com.example.bottomapp.backend

import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

interface IBookInfoExtractor<T> {
    fun getAuthor(file: File): String?
}

class PdfBookAuthorExtractor : IBookInfoExtractor<BookMetaInfo> {
    override fun getAuthor(file: File): String? {
        return extractAuthorFromPDF(file)
    }

    private fun extractAuthorFromPDF(file: File): String? {
        PDDocument.load(file).use { document ->
            val documentInfo = document.documentInformation
            return documentInfo.author
        }
    }
}

class Fb2BookAuthorExtractor : IBookInfoExtractor<BookMetaInfo> {
    override fun getAuthor(file: File): String? {
        return extractAuthorFromFb2(file)
    }

    private fun extractAuthorFromFb2(file: File): String? {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val authorNode = document.getElementsByTagName("author").item(0)
        return authorNode.textContent
    }
}

class DjvuBookAuthorExtractor : IBookInfoExtractor<BookMetaInfo> {
    override fun getAuthor(file: File): String? {
        // DjVu support disabled - would need djvulibre library
        return null
    }
}