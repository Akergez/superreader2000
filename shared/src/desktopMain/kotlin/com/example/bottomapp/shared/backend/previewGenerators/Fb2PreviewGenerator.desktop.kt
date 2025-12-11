package com.example.bottomapp.shared.backend.previewGenerators
actual class Fb2PreviewGenerator : IPreviewGenerator {
    override suspend fun generatePreview(filePath: FileWrapper): Image? {
        // TODO: Implement FB2 preview generation for desktop
        return null
    }
}
