package com.akergez.superreader2000.shared.formats.fb2

import androidx.compose.ui.text.TextMeasurer
import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.formats.abstractions.IRenderHelper
import com.akergez.superreader2000.shared.formats.pdf.TOCEntry
import com.akergez.superreader2000.shared.ui.Fb2BookContent
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

expect class Fb2RenderHelper(
    maxWidth: Int,
    maxHeight: Int,
    textMeasurer: TextMeasurer,
    fb2File: FileWrapper,
) : IRenderHelper<Fb2BookContent> {
    val pageWordSum: MutableList<Int>
    val totalWords: MutableStateFlow<Int?>
    val pagesCount: MutableStateFlow<Int?>
    val tableOfContents: MutableStateFlow<List<TOCEntry>>

    fun getPageIndexByWordOffset(offset: Int): Int
}