package com.example.bottomapp.constants

// Re-export FileType from shared module for backward compatibility
typealias FileType = com.example.bottomapp.shared.backend.domain.FileType

/**
 * Convert FileType to file extension
 */
fun FileType.toFileExtension(): String {
    return when (this) {
        FileType.pdf -> "pdf"
        FileType.fb2 -> "fb2"
        FileType.djvu -> "djvu"
        FileType.unknown -> throw Exception("Unsupported file type")
    }
}

/**
 * Convert FileType to MIME types
 */
fun FileType.toMimeType(): List<String> {
    return when (this) {
        FileType.pdf -> listOf("application/pdf")
        FileType.fb2 -> listOf("application/fb2", "text/fb2+xml")
        FileType.djvu -> listOf("application/djvu")
        FileType.unknown -> throw Exception("Unsupported file type")
    }
}
