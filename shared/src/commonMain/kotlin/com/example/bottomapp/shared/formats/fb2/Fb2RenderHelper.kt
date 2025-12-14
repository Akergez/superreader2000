package com.example.bottomapp.shared.formats.fb2

import androidx.compose.ui.text.TextMeasurer
import com.example.bottomapp.shared.backend.previewGenerators.FileWrapper
import com.example.bottomapp.shared.formats.abstractions.IRenderHelper
import com.example.bottomapp.shared.formats.pdf.TOCEntry
import com.example.bottomapp.shared.ui.Fb2BookContent
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