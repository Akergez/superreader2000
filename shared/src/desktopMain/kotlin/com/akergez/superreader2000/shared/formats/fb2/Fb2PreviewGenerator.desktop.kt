package com.akergez.superreader2000.shared.formats.fb2

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.backend.previewGenerators.Image

actual class Fb2PreviewGenerator : IPreviewGenerator {
    override suspend fun generatePreview(filePath: FileWrapper): Image? {
        // TODO: Implement FB2 preview generation for desktop
        return null
    }
}