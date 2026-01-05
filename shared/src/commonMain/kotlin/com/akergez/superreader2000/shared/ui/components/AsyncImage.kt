// Kotlin
package com.akergez.superreader2000.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AsyncImage(
    uri: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
)
