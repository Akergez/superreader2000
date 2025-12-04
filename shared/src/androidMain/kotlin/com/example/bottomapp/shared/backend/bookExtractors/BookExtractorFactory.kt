package com.example.bottomapp.shared.backend.bookExtractors

import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory



class PdfBookAuthorExtractor : IBookInfoExtractor<BookMetaInfo> {
    private val factory = BookExtractorFactory()

    override fun getAuthor(file: File): String? {
        return runBlocking {
            try {
                val extractor = factory.createPdfExtractor()
                val metadata = extractor.extract(file.absolutePath)
                metadata.author.takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
        }
    }
}
/**
 * Deprecated: Use shared module extractors instead.
 * This class is maintained for backward compatibility only.
 */
@Deprecated("Use shared module BookExtractorFactory instead", ReplaceWith("BookExtractorFactory().createFb2Extractor()"))
class Fb2BookAuthorExtractor : IBookInfoExtractor<BookMetaInfo> {
    private val factory = BookExtractorFactory()

    override fun getAuthor(file: File): String? {
        return runBlocking {
            try {
                val extractor = factory.createFb2Extractor()
                val metadata = extractor.extract(file.absolutePath)
                metadata.author.takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Deprecated: DjVu support is limited. Use shared module extractors instead.
 * This class is maintained for backward compatibility only.
 */
@Deprecated("DjVu support is limited", ReplaceWith("BookExtractorFactory().createDjvuExtractor()"))
class DjvuBookAuthorExtractor : IBookInfoExtractor<BookMetaInfo> {
    override fun getAuthor(file: File): String? {
        // DjVu support disabled - would need djvulibre library
        return null
    }
}


