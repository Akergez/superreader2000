package com.example.bottomapp.shared.backend.previewGenerators

actual class PdfPreviewGenerator : IPreviewGenerator {
    override suspend fun generatePreview(filePath: FileWrapper): Image? {
        // TODO: Implement PDF preview generation for desktop
        return null
    }
}

