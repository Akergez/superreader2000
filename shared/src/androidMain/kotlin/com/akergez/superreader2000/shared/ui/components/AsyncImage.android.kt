// Kotlin
package com.akergez.superreader2000.shared.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.ImageLoader
import coil.compose.AsyncImage
import org.koin.compose.koinInject
import androidx.core.net.toUri

@Composable
actual fun AsyncImage(
    uri: String?,
    modifier: Modifier,
    alpha: Float
) {
    val imageLoader: ImageLoader = koinInject()
    val model = uri?.toUri() ?: uri
    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier,
        alpha = alpha,
        contentScale = ContentScale.Crop,
        imageLoader = imageLoader
    )
}
