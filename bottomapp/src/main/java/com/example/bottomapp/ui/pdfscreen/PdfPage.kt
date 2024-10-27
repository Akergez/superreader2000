package com.example.bottomapp.ui.pdfscreen

import CurlPdfViewer
import PdfRendererHelper
import PdfViewer
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.bottomapp.ui.pdfscreen.pdfflippings.Mode

@Composable
fun PdfPage(
    page: Int,
    pdfRendererHelper: PdfRendererHelper,
    modifier: Modifier = Modifier,
    onToggleMode: (Mode) -> Unit,
    onPageChange: (Int) -> Unit,
    applyInversion: Boolean, // Add this parameter
    mode: Mode,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val haptic = LocalHapticFeedback.current
    var scale by remember { mutableFloatStateOf(1f) } // State to control zoom scale
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isTransforming by remember { mutableStateOf(false) }
    val onZoomModeToggleWrapper = {
        onToggleMode(if (mode == Mode.Zoom) Mode.Curl else Mode.Zoom)
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
    when (mode) {
        Mode.Zoom -> {
            Box(
                modifier = Modifier.basePageBoxModifier()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            isTransforming = true
                            scale = (scale * zoom)
                            offsetX += pan.x
                            offsetY += pan.y
                        }

                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!isTransforming) {
                                    onZoomModeToggleWrapper() // Toggle zoom mode on double-click
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onPress = {
                                tryAwaitRelease() // Wait for gestures to finish
                                isTransforming = false // Reset flag after any gesture is done
                            }
                        )
                    }
            ) {
                // Enable pinch-to-zoom
                PdfViewer(
                    pdfRendererHelper = pdfRendererHelper,
                    pageIndex = page,
                    applyInversion = applyInversion, // Pass the inversion setting
                    scale = scale, // Pass the zoom scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        }
        Mode.Carousel -> {
            Box(
                modifier = Modifier.basePageBoxModifier()
                        .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                onZoomModeToggleWrapper() // Toggle zoom mode on double-click
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleMode(Mode.Curl)
                            }
                        )
                    }
            ) {

                // Enable pinch-to-zoom
                PdfViewer(
                    pdfRendererHelper = pdfRendererHelper,
                    pageIndex = page,
                    applyInversion = applyInversion, // Pass the inversion setting
                    scale = scale, // Pass the zoom scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier.basePageBoxModifier()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                onZoomModeToggleWrapper() // Toggle zoom mode on double-click
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleMode(Mode.Carousel)
                            }
                        )
                    }
            ) {
                // Enable pinch-to-zoom
                CurlPdfViewer(
                    pdfRendererHelper = pdfRendererHelper,
                    pageIndex = page,
                    applyInversion = applyInversion, // Pass the inversion setting
                    scale = scale, // Pass the zoom scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        }
    }
}

@Composable
fun Modifier.basePageBoxModifier(): Modifier {
    return this.padding(8.dp)
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background)
}
