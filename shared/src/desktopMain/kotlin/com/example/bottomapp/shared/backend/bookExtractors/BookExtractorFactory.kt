package com.example.bottomapp.shared.backend.bookExtractors

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

actual class BookExtractorFactory {
    actual fun createPdfExtractor(): IBookInfoExtractor<BookMetaInfo> = DesktopPdfExtractor()
    actual fun createDjvuExtractor(): IBookInfoExtractor<BookMetaInfo> = DesktopDjvuExtractor()
    actual fun createFb2Extractor(): IBookInfoExtractor<BookMetaInfo> = DesktopFb2Extractor()
}

private class DesktopPdfExtractor : IBookInfoExtractor<BookMetaInfo> {
    override suspend fun extract(filePath: String): BookMetaInfo = withContext(Dispatchers.IO) {
        extractPdfMetadata(filePath)
    }

    private fun extractPdfMetadata(filePath: String): BookMetaInfo {
        return try {
            val file = File(filePath)
            val document = Loader.loadPDF(file)
            try {
                val documentInfo = document.documentInformation
                val pageCount = document.numberOfPages

                BookMetaInfo(
                    author = documentInfo?.author ?: "",
                    title = documentInfo?.title ?: file.nameWithoutExtension,
                    subject = documentInfo?.subject ?: "",
                    pageCount = pageCount,
                    producer = documentInfo?.producer ?: ""
                )
            } finally {
                document.close()
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

private class DesktopDjvuExtractor : IBookInfoExtractor<BookMetaInfo> {
    override suspend fun extract(filePath: String): BookMetaInfo = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            // DjVu support on desktop requires native library (djvulibre-jni)
            // Current implementation provides basic metadata from file only
            // TODO: Implement full DjVu support with JNI bindings if needed
            BookMetaInfo(
                title = file.nameWithoutExtension,
                pageCount = 0
            )
        } catch (_: Exception) {
            BookMetaInfo()
        }
    }
}

private class DesktopFb2Extractor : IBookInfoExtractor<BookMetaInfo> {
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

