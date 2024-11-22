package com.example.bottomapp.ui.readerscreens.common

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb


@Composable
fun BitmapCanvas(modifier: Modifier,
                 bitmap: Bitmap,
                 scale: Double,
                 offsetX: Double,
                 offsetY: Double,
                 backgroundColor: Color,
                 applyInversion: Boolean
)
{
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvas = drawContext.canvas.nativeCanvas
        val canvasWidth = size.width.toInt()
        val canvasHeight = size.height.toInt()
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val scaleFactor = scale * minOf(
            canvasWidth.toFloat() / bitmapWidth,
            canvasHeight.toFloat() / bitmapHeight
        )
        val scaledWidth = (bitmapWidth * scaleFactor).toInt()
        val scaledHeight = (bitmapHeight * scaleFactor).toInt()
        val left = (canvasWidth - scaledWidth) / 2 + offsetX.toInt()
        val top = (canvasHeight - scaledHeight) / 2 + offsetY.toInt()

        val destRect = Rect(left, top, left + scaledWidth, top + scaledHeight)

        canvas.drawColor(backgroundColor.toArgb())// Optional background color

        if (applyInversion) {
            val colorMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            val paint = android.graphics.Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            canvas.drawBitmap(bitmap, null, destRect, paint)
        } else {
            canvas.drawBitmap(bitmap, null, destRect, null)
        }
    }
}