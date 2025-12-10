package com.example.bottomapp.shared.backend.previewGenerators

interface IPreviewGenerator {
    suspend fun generatePreview(filePath: FileWrapper): Image?
}

expect class Image {
    val width: Int
    val height: Int
}

expect class FileWrapper {

}