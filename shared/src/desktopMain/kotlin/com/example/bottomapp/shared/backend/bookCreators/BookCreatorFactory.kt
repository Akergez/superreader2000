package com.example.bottomapp.shared.backend.bookCreators

import com.example.bottomapp.shared.backend.domain.BookMetadata
import com.example.bottomapp.shared.backend.domain.PdfBookMetadata
import com.example.bottomapp.shared.backend.domain.DjvuBookMetadata
import com.example.bottomapp.shared.backend.domain.Fb2BookMetadata
import com.example.bottomapp.shared.backend.domain.FormatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

actual class BookCreatorFactory {
    actual fun createPdfFactory(): IBookCreateFactory = DesktopPdfCreateFactory()
    actual fun createDjvuFactory(): IBookCreateFactory = DesktopDjvuCreateFactory()
    actual fun createFb2Factory(): IBookCreateFactory = DesktopFb2CreateFactory()
}

private class DesktopPdfCreateFactory : IBookCreateFactory {
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

private class DesktopDjvuCreateFactory : IBookCreateFactory {
    override suspend fun create(filePath: String): BookMetadata? {
        // DjVu support on desktop requires native library or JNI bindings
        // TODO: Implement DjVu rendering for desktop
        // For now, return a stub
        val file = File(filePath)
        return DjvuBookMetadata(
            uri = file.absolutePath,
            name = file.nameWithoutExtension,
            formatType = FormatType.DJVU,
            pageCount = 0,
            title = file.nameWithoutExtension
        )
    }
}

private class DesktopFb2CreateFactory : IBookCreateFactory {
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

