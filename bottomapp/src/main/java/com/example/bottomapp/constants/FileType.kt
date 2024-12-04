package com.example.bottomapp.constants

enum class FileType {
    pdf,
    fb2,
    unknown
}

fun FileType.toMimeType(): String {
    return when (this) {
        FileType.pdf -> "pdf"
        FileType.fb2 -> "fb2"
        FileType.unknown -> throw Exception("Unsupported file type")
    }
}
