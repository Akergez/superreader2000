/*
package com.example.bottomapp.ui.readerscreens

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Key(
    val width: Int,
    val height: Int,
    val applyInversion: Boolean,
    val backgroundColor: Color,
    val key: Bitmap
)

class CanvasCache {
    val cache = mutableMapOf<Key, StateFlow<Bitmap?>>()

    fun getCanvas(
        width: Int,
        height: Int,
        applyInversion: Boolean,
        backgroundColor: Color,
        bitmap: Bitmap
    ): StateFlow<Bitmap?> {
        val cacheKey = Key(width, height, applyInversion, backgroundColor, bitmap);
        if (cache.containsKey(cacheKey)) {
            return cache[cacheKey]!!
        } else {
            val flow = MutableStateFlow<Bitmap?>(null);
            CoroutineScope(Dispatchers.IO).launch {
                val renderedBitmap: Bitmap? =
                    renderBitmap(width, height, applyInversion, backgroundColor, bitmap);
                withContext(Dispatchers.Main) {
                    flow.value = renderedBitmap;
                }
            }
            return flow;
        }
    }


}*/
