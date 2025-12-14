package com.example.bottomapp.shared.formats.fb2

import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.Image

actual class Fb2PreviewGenerator : IPreviewGenerator {
    override suspend fun generatePreview(filePath: FileWrapper): Image? {
        // TODO: Implement FB2 preview generation for desktop
        return null
    }
}