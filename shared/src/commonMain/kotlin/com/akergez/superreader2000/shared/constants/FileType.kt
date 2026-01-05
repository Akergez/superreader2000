package com.akergez.superreader2000.shared.constants

import com.akergez.superreader2000.shared.formats.abstractions.FileType

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
