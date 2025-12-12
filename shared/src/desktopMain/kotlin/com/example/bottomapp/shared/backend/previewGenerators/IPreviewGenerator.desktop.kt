package com.example.bottomapp.shared.backend.previewGenerators

import java.awt.image.BufferedImage
import java.io.File

actual class FileWrapper(val file: File)

actual class Image(val bufferedImage: BufferedImage) {
    actual val width: Int
        get() = bufferedImage.width
    actual val height: Int
        get() = bufferedImage.height
}