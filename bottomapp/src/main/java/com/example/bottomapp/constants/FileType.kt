package com.example.bottomapp.constants

enum class FileType {
    pdf,
    fb2,
    djvu,
    unknown
}

fun FileType.toFileExtension(): String {
    return when (this) {
        FileType.pdf -> "pdf"
        FileType.fb2 -> "fb2"
        FileType.djvu -> "djvu"
        FileType.unknown -> throw Exception("Unsupported file type")
    }
}

fun FileType.toMimeType(): List<String> {
    return when (this) {
        FileType.pdf -> listOf("application/pdf")
        FileType.fb2 -> listOf("application/fb2", "text/fb2+xml")
        FileType.djvu -> listOf("application/djvu")
        FileType.unknown -> throw Exception("Unsupported file type")
    }
}
