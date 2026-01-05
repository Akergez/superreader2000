package com.akergez.superreader2000.shared.formats.fb2

import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.IBookInfoExtractor
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

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