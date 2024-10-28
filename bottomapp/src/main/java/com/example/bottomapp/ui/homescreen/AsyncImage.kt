package com.example.bottomapp.ui.homescreen

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

@Composable
fun AsyncImage(
    uri: Uri?,
    animationDurationMillis: Int = 0,
    modifier: Modifier,
    alpha: Float = 1f,
) {
    coil.compose.AsyncImage(
        model = uri,
        contentDescription = null,
        modifier = modifier,
        alpha = alpha,
        contentScale = ContentScale.Crop
    )
}