package com.example.bottomapp.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    if (enabled) {
        DisposableEffect(enabled, onBack) {
            val listener = object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    if (e.keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
                        onBack()
                        e.consume()
                    }
                }
            }

            try {
                val window = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
                window?.addKeyListener(listener)

                onDispose {
                    window?.removeKeyListener(listener)
                }
            } catch (_: Exception) {
                onDispose { }
            }
        }
    }
}

