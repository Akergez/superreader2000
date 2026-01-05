package com.akergez.superreader2000.shared.backend.previewGenerators

import java.io.File

interface IPreviewGenerator {
    suspend fun generatePreview(filePath: FileWrapper): Image?
}

expect class Image {
    val width: Int
    val height: Int
}

expect class FileWrapper(file: File) {
    val file: File
}
