package com.akergez.superreader2000.shared.formats.pdf

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.formats.abstractions.IRenderHelper
import com.akergez.superreader2000.shared.ui.PdfBookContent
import kotlinx.coroutines.flow.MutableStateFlow

expect class PdfRendererHelper(pdfFile: suspend () -> FileWrapper): IRenderHelper<PdfBookContent>
{
    val pagesCount : MutableStateFlow<Int?>
}

data class TOCEntry(
    val title: String,
    val pageIndex: Int,
    val children: List<TOCEntry>,
    /** Stable navigation target; FB2 uses a word offset while fixed-layout formats use a page. */
    val navigationIndex: Int = pageIndex,
)

data class bitmapKey(
    val applyInversion: Boolean,
    val pageIndex: Int
)
