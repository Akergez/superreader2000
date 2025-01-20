package com.example.bottomapp.backend.previewGenerators

import android.graphics.Bitmap
import java.io.File
import java.util.Locale

interface IPreviewGenerator {
    fun generatePreviewBitmap(file: File): Bitmap?
}