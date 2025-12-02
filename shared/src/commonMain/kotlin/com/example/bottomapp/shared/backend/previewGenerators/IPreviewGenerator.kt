package com.example.bottomapp.shared.backend.previewGenerators

import com.example.bottomapp.shared.platform.PlatformBitmap

/**
 * Common interface for all preview generators
 */
interface IPreviewGenerator {
    suspend fun generatePreviewBitmap(filePath: String): PlatformBitmap?
}

/**
 * Platform-specific factory for creating preview generators
 */
expect class PreviewGeneratorFactory() {
    fun createPdfPreviewGenerator(): IPreviewGenerator
    fun createDjvuPreviewGenerator(): IPreviewGenerator
    fun createFb2PreviewGenerator(): IPreviewGenerator
}

