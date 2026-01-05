package com.akergez.superreader2000.shared.backend.previewGenerators

import java.awt.image.BufferedImage
import java.io.File

actual class FileWrapper actual constructor(actual val file: File)

actual class Image(val bufferedImage: BufferedImage) {
    actual val width: Int
        get() = bufferedImage.width
    actual val height: Int
        get() = bufferedImage.height
}