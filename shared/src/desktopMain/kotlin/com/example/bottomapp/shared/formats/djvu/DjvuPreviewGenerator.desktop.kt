package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.Image

actual class DjvuPreviewGenerator :
    IPreviewGenerator {
    actual override suspend fun generatePreview(filePath: FileWrapper): Image? {
        TODO("Not yet implemented")
    }
}