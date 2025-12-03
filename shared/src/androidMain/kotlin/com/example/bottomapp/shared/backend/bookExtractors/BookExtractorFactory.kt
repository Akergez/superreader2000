package com.example.bottomapp.shared.backend.bookExtractors

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

actual class BookExtractorFactory {
    actual fun createPdfExtractor(): IBookInfoExtractor<BookMetaInfo> = AndroidPdfExtractor()
    actual fun createDjvuExtractor(): IBookInfoExtractor<BookMetaInfo> = AndroidDjvuExtractor()
    actual fun createFb2Extractor(): IBookInfoExtractor<BookMetaInfo> = AndroidFb2Extractor()
}

private class AndroidPdfExtractor : IBookInfoExtractor<BookMetaInfo> {
    override suspend fun extract(filePath: String): BookMetaInfo = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            PDDocument.load(file).use { document ->
                val documentInfo = document.documentInformation
                val pageCount = document.numberOfPages

                BookMetaInfo(
                    author = documentInfo?.author ?: "",
                    title = documentInfo?.title ?: file.nameWithoutExtension,
                    subject = documentInfo?.subject ?: "",
                    pageCount = pageCount,
                    producer = documentInfo?.producer ?: ""
                )
            }
        } catch (_: Exception) {
            // Gracefully handle corrupted or unreadable PDFs
            val file = File(filePath)
            BookMetaInfo(
                title = file.nameWithoutExtension,
                pageCount = 0
            )
        }
    }
}

private class AndroidDjvuExtractor : IBookInfoExtractor<BookMetaInfo> {
    override suspend fun extract(filePath: String): BookMetaInfo = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            // DjVu support on Android requires djvulibre-android library
            // For now, provide basic metadata from file
            BookMetaInfo(
                title = file.nameWithoutExtension,
                pageCount = 0
            )
        } catch (_: Exception) {
            BookMetaInfo()
        }
    }
}

private class AndroidFb2Extractor : IBookInfoExtractor<BookMetaInfo> {
    override suspend fun extract(filePath: String): BookMetaInfo = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            val document = DocumentBuilderFactory.newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(file)

            // Extract book title
            val titleNodeList = document.getElementsByTagName("book-title")
            val title = if (titleNodeList.length > 0) {
                titleNodeList.item(0)?.textContent ?: file.nameWithoutExtension
            } else {
                file.nameWithoutExtension
            }

            // Extract author (first author if multiple exist)
            val authorNodeList = document.getElementsByTagName("author")
            var author = ""
            if (authorNodeList.length > 0) {
                val authorNode = authorNodeList.item(0)
                val firstNameList = document.getElementsByTagName("first-name")
                val lastNameList = document.getElementsByTagName("last-name")
                val firstName = if (firstNameList.length > 0) {
                    firstNameList.item(0)?.textContent ?: ""
                } else {
                    ""
                }
                val lastName = if (lastNameList.length > 0) {
                    lastNameList.item(0)?.textContent ?: ""
                } else {
                    ""
                }
                author = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                    "$firstName $lastName".trim()
                } else {
                    ""
                }
            }

            BookMetaInfo(
                author = author,
                title = title,
                pageCount = estimateFb2PageCount(file)
            )
        } catch (_: Exception) {
            // Fallback if XML parsing fails
            val file = File(filePath)
            BookMetaInfo(
                title = file.nameWithoutExtension,
                pageCount = 0
            )
        }
    }

    private suspend fun estimateFb2PageCount(file: File): Int {
        return withContext(Dispatchers.IO) {
            try {
                val content = file.readText()
                // Rough estimate: assume ~2000 characters per page
                (content.length / 2000) + 1
            } catch (_: Exception) {
                0
            }
        }
    }
}

