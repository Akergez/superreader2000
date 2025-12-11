package com.example.bottomapp.shared.ui.components

import androidx.compose.runtime.Composable

/**
 * Platform-specific back handler.
 * On Android: handles system back button press
 * On iOS/Desktop: no-op (no system back button)
 */
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)

