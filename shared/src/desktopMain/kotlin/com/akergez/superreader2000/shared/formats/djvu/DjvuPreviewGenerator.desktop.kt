package com.akergez.superreader2000.shared.formats.djvu

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.backend.previewGenerators.Image

actual class DjvuPreviewGenerator :
    IPreviewGenerator {
    actual override suspend fun generatePreview(filePath: FileWrapper): Image? {
        TODO("Not yet implemented")
    }
}