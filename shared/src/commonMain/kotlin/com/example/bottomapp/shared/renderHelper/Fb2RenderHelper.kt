package com.example.bottomapp.shared.renderHelper

import com.example.bottomapp.shared.ui.Fb2BookContent
import kotlinx.coroutines.flow.MutableStateFlow

expect class Fb2RenderHelper : IRenderHelper<Fb2BookContent> {
    val pageWordSum: MutableList<Int>
    val totalWords: MutableStateFlow<Int?>
    val pagesCount: MutableStateFlow<Int?>
    val tableOfContents: MutableStateFlow<List<TOCEntry>>

    fun getPageIndexByWordOffset(offset: Int): Int
}