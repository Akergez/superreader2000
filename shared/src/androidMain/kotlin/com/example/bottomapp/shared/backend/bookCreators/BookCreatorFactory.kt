package com.example.bottomapp.shared.backend.bookCreators

import com.example.bottomapp.shared.backend.domain.BookMetadata
import com.example.bottomapp.shared.backend.domain.DjvuBookMetadata
import com.example.bottomapp.shared.backend.domain.Fb2BookMetadata
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.shared.backend.domain.PdfBookMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class BookCreatorFactory {
    actual fun createPdfFactory(): IBookCreateFactory = AndroidPdfCreateFactory()
    actual fun createDjvuFactory(): IBookCreateFactory = AndroidDjvuCreateFactory()
    actual fun createFb2Factory(): IBookCreateFactory = AndroidFb2CreateFactory()
}

private class AndroidPdfCreateFactory : IBookCreateFactory {
    override suspend fun create(filePath: String): BookMetadata? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            // Basic implementation - just get file info without full PDF parsing
            val book = PdfBookMetadata(
                uri = file.absolutePath,
                name = file.nameWithoutExtension,
                formatType = FormatType.PDF,
                pageCount = 0,  // Default, would need PDF parsing library
                author = "",
                title = file.nameWithoutExtension,
                subject = "",
                producer = ""
            )
            book
        } catch (e: Exception) {
            null
        }
    }
}

private class AndroidDjvuCreateFactory : IBookCreateFactory {
    override suspend fun create(filePath: String): BookMetadata? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            DjvuBookMetadata(
                uri = file.absolutePath,
                name = file.nameWithoutExtension,
                formatType = FormatType.DJVU,
                pageCount = 0,  // Would need DjVu library
                title = file.nameWithoutExtension
            )
        } catch (e: Exception) {
            null
        }
    }
}

private class AndroidFb2CreateFactory : IBookCreateFactory {
    override suspend fun create(filePath: String): BookMetadata? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            val content = file.readText()
            // Simple XML parsing for FB2
            val titleMatch = Regex("<book-title>([^<]+)</book-title>").find(content)
            val authorMatch = Regex("<first-name>([^<]+)</first-name>").find(content)

            Fb2BookMetadata(
                uri = file.absolutePath,
                name = file.nameWithoutExtension,
                formatType = FormatType.FB2,
                pageCount = estimateFb2PageCount(content),
                title = titleMatch?.groupValues?.get(1) ?: file.nameWithoutExtension,
                author = authorMatch?.groupValues?.get(1) ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun estimateFb2PageCount(content: String): Int {
        // Rough estimate based on content length
        return (content.length / 2000) + 1
    }
}

