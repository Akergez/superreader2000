package com.example.bottomapp.shared.backend.previewGenerators

import android.graphics.Bitmap
import java.io.File

actual class Image(val bitmap: Bitmap) {
    actual val width: Int get() = bitmap.width
    actual val height: Int get() = bitmap.height
}

actual class FileWrapper actual constructor(actual val file: File)
