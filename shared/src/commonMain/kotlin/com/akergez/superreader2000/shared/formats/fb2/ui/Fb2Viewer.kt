package com.akergez.superreader2000.shared.formats.fb2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akergez.superreader2000.shared.ui.DrawCommand
import com.akergez.superreader2000.shared.ui.Fb2BookContent
import org.jetbrains.compose.resources.stringResource
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.loading

@Composable
fun Fb2Viewer(
    content: Fb2BookContent?,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
) {
    val backgroundColor = if (applyInversion) Color.Black else Color.White
    val textColor = if (applyInversion) Color.White else Color.Black

    val baseModifier = modifier
        .background(backgroundColor)
        .fillMaxSize()

    if (content == null || content.drawCommands.isEmpty()) {
        Text(
            text = stringResource(Res.string.loading),
            color = textColor,
            modifier = baseModifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Justify),
        )
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
                content.drawCommands.forEach { cmd ->
                    when (cmd) {
                        is DrawCommand.DrawText -> {
                            val style = cmd.style.copy(
                                color = resolveTextColor(cmd.style.color, textColor)
                            )
                            drawText(
                                textMeasurer = textMeasurer,
                                text         = cmd.text,
                                topLeft      = Offset(cmd.x, cmd.y - style.fontSize.toPx()),
                                style        = style,
                                size         = layoutSize,
                            )
                        }
                        is DrawCommand.DrawInlineImage -> {
                            // TODO: drawImage когда ImageBitmap будет доступен из imageMap
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
