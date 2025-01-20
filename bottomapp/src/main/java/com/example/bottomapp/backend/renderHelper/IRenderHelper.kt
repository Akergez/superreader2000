package com.example.bottomapp.backend.renderHelper

import com.example.bottomapp.backend.FormatType
import com.example.bottomapp.ui.readerscreens.IBookContent
import kotlinx.coroutines.flow.StateFlow

interface IRenderHelper {
    fun openPage(pageIndex: Int, applyInversion: Boolean): StateFlow<IBookContent>
    fun close()
    fun getBookType(): FormatType
    fun getTableOfContents(): StateFlow<List<TOCEntry>>
}