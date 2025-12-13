package com.example.bottomapp.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.net.URL

@Composable
actual fun AsyncImage(
    uri: String?,
    modifier: Modifier,
    alpha: Float
) {
    var imageBitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri != null) {
            launch {
                imageBitmap = withContext(Dispatchers.IO) {
                    try {
                        val bytes = URL(uri).readBytes()
                        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
    }

    imageBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alpha = alpha
        )
    }
}
