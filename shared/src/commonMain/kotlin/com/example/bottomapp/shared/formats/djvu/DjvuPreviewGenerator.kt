package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.Image

expect class DjvuPreviewGenerator : IPreviewGenerator {
    override suspend fun generatePreview(filePath: FileWrapper): Image?
}