package com.example.bottomapp.backend.renderHelper

import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.ui.readerscreens.IBookContent
import kotlinx.coroutines.flow.StateFlow

interface IRenderHelper<T>
        where T : IBookContent {
    fun openPage(pageIndex: Int, applyInversion: Boolean): StateFlow<T>
    fun close()
    fun getBookType(): FormatType
    fun getTableOfContents(): StateFlow<List<TOCEntry>>
}