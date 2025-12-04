package com.example.bottomapp.shared.backend.bookExtractors

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Interface for extracting book metadata information
 */
interface IBookInfoExtractor<T> {
    fun getAuthor(file: File): String?
}

/**
 * Platform-specific factory for creating extractors
 */
class PdfBookAuthorExtractor : IBookInfoExtractor<BookMetaInfo> {
    override fun getAuthor(file: File): String? {
        return extractAuthorFromPDF(file)
    }

    private fun extractAuthorFromPDF(file: File): String? {
        /*        PDDocument.load(file).use { document ->
                    val documentInfo = document.documentInformation
                    return documentInfo.author
                }*/
        //ToDo implement
        return ""
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
        /*        val djvu = DjvuLibre(FileInputStream(file).fd)
        return djvu.author*/
        return ""
        //ToDo implement
    }
}


