package com.example.bottomapp.ui.readerscreens.fb2screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.FlipMode


@Composable
fun Fb2Page(
    pageContent: String,
    mode: FlipMode,
    onToggleMode: (FlipMode) -> Unit,
    settingsManager: SettingsManager
) {
    val haptic = LocalHapticFeedback.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isTransforming by remember { mutableStateOf(false) }
    // Настройки оформления
    val fontSize = 16 //settingsManager.getFontSize()

    val backgroundColor = if (settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert)
        Color.Black else Color.White
    val textColor = if (settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert)
        Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .let {
                if (mode == FlipMode.Zoom) {
                    it.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            isTransforming = true
                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                } else it
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isTransforming) {
                            onToggleMode(if (mode == FlipMode.Zoom) FlipMode.Curl else FlipMode.Zoom)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleMode(if (mode == FlipMode.Curl) FlipMode.Carousel else FlipMode.Curl )
                    }
                )
            }
    ) {
        Text(
            text = pageContent,
            fontSize = fontSize.sp * scale,
            color = textColor,
            modifier = Modifier.offset(offsetX.dp, offsetY.dp),
            textAlign = TextAlign.Left
        )
    }
}