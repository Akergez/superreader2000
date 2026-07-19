package com.akergez.superreader2000.shared.formats.fb2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Constraints
import com.akergez.superreader2000.shared.ui.DrawCommand
import com.akergez.superreader2000.shared.ui.Fb2BookContent
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode

@Composable
fun Fb2Viewer(
    content: Fb2BookContent?,
    modifier: Modifier = Modifier,
    documentTheme: DocThemeMode,
    compatibilityTextRenderer: Boolean,
) {
    val (backgroundColor, textColor) = when (documentTheme) {
        DocThemeMode.AsIs -> Color.White to Color.Black
        DocThemeMode.Invert -> Color.Black to Color.White
        DocThemeMode.SoftLight -> Color(0xFFD6D3CC) to Color(0xFF353535)
        DocThemeMode.SoftDark -> Color(0xFF242424) to Color(0xFFD0D0D0)
        DocThemeMode.MaterialYou -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
    }

    val baseModifier = modifier
        .background(backgroundColor)
        .fillMaxSize()

    if (content == null || content.drawCommands.isEmpty()) {
        Box(modifier = baseModifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()

    Box(modifier = baseModifier.clipToBounds()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val nativeW = content.nativeWidth.toFloat()
            val nativeH = content.nativeHeight.toFloat()

            // When the canvas is smaller than the native page (carousel, two-page mode),
            // scale the drawing down. Always pass the native size to drawText so that
            // text is measured at the original line width and never re-wraps.
            val drawScale = if (nativeW > 0f && nativeH > 0f)
                minOf(size.width / nativeW, size.height / nativeH)
            else 1f

            val layoutSize = if (nativeW > 0f && nativeH > 0f)
                Size(nativeW, nativeH)
            else
                Size(size.width, size.height)

            withTransform({ scale(drawScale, drawScale, Offset.Zero) }) {
                val inlineImages = content.drawCommands.filterIsInstance<DrawCommand.DrawInlineImage>()
                val textHasPlaceholders = content.drawCommands
                    .filterIsInstance<DrawCommand.DrawText>()
                    .any { it.placeholders.isNotEmpty() }
                content.drawCommands.forEach { cmd ->
                    when (cmd) {
                        is DrawCommand.DrawText -> {
                            val style = cmd.style.copy(
                                color = resolveTextColor(cmd.style.color, textColor)
                            )
                            val width = if (cmd.textAreaWidth > 0f) cmd.textAreaWidth
                                        else layoutSize.width - cmd.x
                            val height = layoutSize.height - cmd.y
                            if (cmd.placeholders.isNotEmpty()) {
                                // Text and images must use one layout. Measuring the text
                                // again inside drawText while keeping image coordinates from
                                // pagination lets platform engines place them differently.
                                val pageLayout = textMeasurer.measure(
                                    text = cmd.text,
                                    style = style,
                                    constraints = Constraints(
                                        minWidth = width.toInt(),
                                        maxWidth = width.toInt(),
                                        minHeight = 0,
                                        maxHeight = height.toInt().coerceAtLeast(1),
                                    ),
                                    softWrap = true,
                                    placeholders = cmd.placeholders,
                                )
                                drawText(pageLayout, topLeft = Offset(cmd.x, cmd.y))
                                inlineImages.forEachIndexed { index, imageCommand ->
                                    val rect = pageLayout.placeholderRects.getOrNull(index)
                                        ?: return@forEachIndexed
                                    val bitmap = content.imageMap[imageCommand.imageId]
                                        ?: return@forEachIndexed
                                    drawImage(
                                        image = bitmap,
                                        dstOffset = IntOffset(
                                            (cmd.x + rect.left).toInt(),
                                            (cmd.y + rect.top).toInt(),
                                        ),
                                        dstSize = IntSize(
                                            rect.width.toInt().coerceAtLeast(1),
                                            rect.height.toInt().coerceAtLeast(1),
                                        ),
                                    )
                                }
                            } else {
                                drawFb2Text(
                                    command = cmd,
                                    textMeasurer = textMeasurer,
                                    style = style,
                                    width = width,
                                    height = height,
                                    nativeJustificationSupported = !compatibilityTextRenderer,
                                )
                            }
                        }
                        is DrawCommand.DrawInlineImage -> {
                            if (textHasPlaceholders) return@forEach
                            val bitmap = content.imageMap[cmd.imageId] ?: return@forEach
                            drawImage(
                                image     = bitmap,
                                dstOffset = IntOffset(cmd.x.toInt(), cmd.y.toInt()),
                                dstSize   = IntSize(cmd.width.toInt(), cmd.height.toInt()),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveTextColor(color: Color, textColor: Color): Color {
    if (color == Color.Unspecified) return textColor
    val isDark = textColor == Color.White
    return if (isDark && color.luminance() < 0.1f) Color.White else color
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
