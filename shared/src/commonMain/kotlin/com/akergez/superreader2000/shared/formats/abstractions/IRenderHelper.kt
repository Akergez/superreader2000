package com.akergez.superreader2000.shared.formats.abstractions

import com.akergez.superreader2000.shared.formats.pdf.TOCEntry
import com.akergez.superreader2000.shared.ui.IBookContent
import kotlinx.coroutines.flow.StateFlow

interface IRenderHelper<T>
        where T : IBookContent {
    fun openPage(pageIndex: Int, applyInversion: Boolean): StateFlow<T>
    fun close()
    fun getBookType(): FormatType
    fun getTableOfContents(): StateFlow<List<TOCEntry>>
}