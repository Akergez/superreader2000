package com.akergez.superreader2000.shared.ui

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle

interface IBookContent {
    val nativeWidth: Int get() = 0
    val nativeHeight: Int get() = 0
}

data class PdfBookContent(val bitmap: ImageBitmap?): IBookContent
data class DjvuBookContent(val bitmap: ImageBitmap?): IBookContent

data class Fb2BookContent(
    val drawCommands: List<DrawCommand>,
    val imageMap: Map<String, InlineTextContent>,
    val pageIndex: Int,
    val totalPages: Int?,
    /** Размер страницы в пикселях, для которого рассчитаны drawCommands. */
    override val nativeWidth: Int = 0,
    override val nativeHeight: Int = 0,
) : IBookContent

sealed class DrawCommand {
    /**
     * Один отрезок текста с уже применёнными спанами (bold/italic/color/…).
     * [x], [y] — координаты baseline в системе координат страницы.
     * Зум применяется снаружи через graphicsLayer — текст всегда чёткий.
     */
    data class DrawText(
        val text: AnnotatedString,
        val x: Float,
        val y: Float,
        val style: TextStyle,
    ) : DrawCommand()

    /**
     * Инлайн-изображение (картинки остаются растром — это нормально).
     */
    data class DrawInlineImage(
        val imageId: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    ) : DrawCommand()
}