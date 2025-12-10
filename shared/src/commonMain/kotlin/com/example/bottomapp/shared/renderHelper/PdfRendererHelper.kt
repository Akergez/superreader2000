package com.example.bottomapp.shared.renderHelper

import com.example.bottomapp.shared.ui.PdfBookContent
import kotlinx.coroutines.flow.MutableStateFlow

expect class PdfRendererHelper : IRenderHelper<PdfBookContent>
{
    val pagesCount : MutableStateFlow<Int?>
}

data class TOCEntry(
    val title: String,
    val pageIndex: Int,
    val children: List<TOCEntry>
)

data class bitmapKey(
    val applyInversion: Boolean,
    val pageIndex: Int
)