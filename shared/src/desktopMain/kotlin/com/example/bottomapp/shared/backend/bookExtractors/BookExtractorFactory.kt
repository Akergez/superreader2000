package com.example.bottomapp.shared.backend.bookExtractors

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

actual class BookExtractorFactory {
    actual fun createPdfExtractor(): IBookInfoExtractor<BookMetaInfo> = DesktopPdfExtractor()
    actual fun createDjvuExtractor(): IBookInfoExtractor<BookMetaInfo> = DesktopDjvuExtractor()
    actual fun createFb2Extractor(): IBookInfoExtractor<BookMetaInfo> = DesktopFb2Extractor()
}

private class DesktopPdfExtractor : IBookInfoExtractor<BookMetaInfo> {
    override suspend fun extract(filePath: String): BookMetaInfo = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            // Basic implementation without full PDF parsing
            BookMetaInfo(
                author = "",
                title = file.nameWithoutExtension,
                subject = "",
                pageCount = 0,
                producer = ""
            )
        } catch (e: Exception) {
            BookMetaInfo()
        }
    }
}

private class DesktopDjvuExtractor : IBookInfoExtractor<BookMetaInfo> {
    override suspend fun extract(filePath: String): BookMetaInfo {
        // DjVu support on desktop requires native library or JNI bindings
        // TODO: Implement DjVu extraction for desktop
        val file = File(filePath)
        return BookMetaInfo(
            title = file.nameWithoutExtension
        )
    }
}

private class DesktopFb2Extractor : IBookInfoExtractor<BookMetaInfo> {
    override suspend fun extract(filePath: String): BookMetaInfo = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            val content = file.readText()
            val titleMatch = Regex("<book-title>([^<]+)</book-title>").find(content)
            val authorMatch = Regex("<first-name>([^<]+)</first-name>").find(content)

            BookMetaInfo(
                author = authorMatch?.groupValues?.get(1) ?: "",
                title = titleMatch?.groupValues?.get(1) ?: file.nameWithoutExtension,
                pageCount = estimateFb2PageCount(content)
            )
        } catch (e: Exception) {
            BookMetaInfo()
        }
    }

    private fun estimateFb2PageCount(content: String): Int {
        return (content.length / 2000) + 1
    }
}

