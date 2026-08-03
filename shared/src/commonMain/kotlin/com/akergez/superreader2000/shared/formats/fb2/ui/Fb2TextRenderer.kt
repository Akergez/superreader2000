package com.akergez.superreader2000.shared.formats.fb2.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import com.akergez.superreader2000.shared.ui.DrawCommand

private const val GEOMETRY_TOLERANCE_PX = 1f

internal fun DrawScope.drawFb2Text(
    command: DrawCommand.DrawText,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    width: Float,
    height: Float,
    nativeJustificationSupported: Boolean,
) {
    if (nativeJustificationSupported || command.placeholders.isNotEmpty()) {
        drawText(
            textMeasurer = textMeasurer,
            text = command.text,
            topLeft = Offset(command.x, command.y),
            style = style,
            size = Size(width, height),
            placeholders = command.placeholders,
        )
        return
    }

    // textAlign здесь обязан совпадать с тем, которым страницу нарезала пагинация
    // (RenderConfig.textStyle в compat-режиме уже Start): line breaker на Android
    // учитывает режим выравнивания, и Justify-разметка даёт другие переносы.
    val layout = textMeasurer.measure(
        text = command.text,
        style = style.copy(textAlign = TextAlign.Start),
        constraints = Constraints.fixedWidth(width.toInt()),
        softWrap = true,
    )

    for (line in 0 until layout.lineCount) {
        // Строка, не влезающая в страницу, рисовалась бы за нижним краем канваса.
        // Такой текст всё равно не виден, а следующая страница начинается со своей
        // границы — обрезанные строки просто пропали бы из книги.
        if (layout.getLineTop(line) >= height) break
        val start = layout.getLineStart(line)
        val end = layout.getLineEnd(line, visibleEnd = true)
        if (end <= start) continue

        val source = command.text.subSequence(start, end).withoutParagraphStyles()
        val paragraphAlignment = command.text.paragraphStyles
            .lastOrNull { start >= it.start && start < it.end }
            ?.item
            ?.textAlign
            ?: TextAlign.Justify
        val hardBreak = end < command.text.length && command.text[end] == '\n'
        val softWrapped = !hardBreak && end < command.text.length
        val missingWidth = width - layout.getLineRight(line)
        val rendered = if (paragraphAlignment == TextAlign.Justify && softWrapped) {
            justify(source, missingWidth)
        } else {
            source
        }

        drawText(
            textMeasurer = textMeasurer,
            text = rendered,
            topLeft = Offset(command.x, command.y + layout.getLineTop(line)),
            style = style.copy(
                textAlign = if (paragraphAlignment == TextAlign.Justify) {
                    TextAlign.Start
                } else {
                    paragraphAlignment
                }
            ),
            size = Size(width, (height - layout.getLineTop(line)).coerceAtLeast(0f)),
            softWrap = false,
        )
    }
}

private fun DrawScope.justify(line: AnnotatedString, missingWidth: Float): AnnotatedString {
    val spaces = line.text.count { it == ' ' }
    if (spaces == 0 || missingWidth <= GEOMETRY_TOLERANCE_PX) return line

    val extraSpacing = (missingWidth / spaces).toSp()
    return buildAnnotatedString {
        append(line)
        line.text.forEachIndexed { index, character ->
            if (character == ' ') addStyle(SpanStyle(letterSpacing = extraSpacing), index, index + 1)
        }
    }
}

private fun AnnotatedString.withoutParagraphStyles() = AnnotatedString(text, spanStyles)
