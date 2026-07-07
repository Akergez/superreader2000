package com.akergez.superreader2000.shared.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextStyle

interface IBookContent {
    val nativeWidth: Int get() = 0
    val nativeHeight: Int get() = 0
}

data class PdfBookContent(val bitmap: ImageBitmap?): IBookContent {
    override val nativeWidth: Int get() = bitmap?.width ?: 0
    override val nativeHeight: Int get() = bitmap?.height ?: 0
}
data class DjvuBookContent(val bitmap: ImageBitmap?): IBookContent {
    override val nativeWidth: Int get() = bitmap?.width ?: 0
    override val nativeHeight: Int get() = bitmap?.height ?: 0
}

data class Fb2BookContent(
    val drawCommands: List<DrawCommand>,
    val imageMap: Map<String, ImageBitmap>,
    val pageIndex: Int,
    val totalPages: Int?,
    /** Размер страницы в пикселях, для которого рассчитаны drawCommands. */
    override val nativeWidth: Int = 0,
    override val nativeHeight: Int = 0,
) : IBookContent

sealed class DrawCommand {
    /**
     * Блок текста страницы.
     * [x], [y] — координаты верхнего левого угла текстовой области.
     * [textAreaWidth] — ширина области для верстки (без отступов); 0 = fallback на всю ширину canvas.
     */
    data class DrawText(
        val text: AnnotatedString,
        val x: Float,
        val y: Float,
        val style: TextStyle,
        val textAreaWidth: Float = 0f,
        val placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList(),
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