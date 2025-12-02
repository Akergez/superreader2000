package com.example.bottomapp.shared.backend.bookExtractors

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.github.axet.djvulibre.DjvuLibre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileInputStream

actual class BookExtractorFactory {
    actual fun createPdfExtractor(): IBookInfoExtractor<BookMetaInfo> = AndroidPdfExtractor()
    actual fun createDjvuExtractor(): IBookInfoExtractor<BookMetaInfo> = AndroidDjvuExtractor()
    actual fun createFb2Extractor(): IBookInfoExtractor<BookMetaInfo> = AndroidFb2Extractor()
}

private class AndroidPdfExtractor : IBookInfoExtractor<BookMetaInfo> {
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

private class AndroidDjvuExtractor : IBookInfoExtractor<BookMetaInfo> {
    override suspend fun extract(filePath: String): BookMetaInfo = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            BookMetaInfo(
                title = file.nameWithoutExtension,
                pageCount = 0
            )
        } catch (e: Exception) {
            BookMetaInfo()
        }
    }
}

private class AndroidFb2Extractor : IBookInfoExtractor<BookMetaInfo> {
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

