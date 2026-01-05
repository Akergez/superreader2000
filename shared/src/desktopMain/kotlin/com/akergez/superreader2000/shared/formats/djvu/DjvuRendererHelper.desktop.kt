package com.akergez.superreader2000.shared.formats.djvu

import com.akergez.superreader2000.shared.formats.abstractions.FormatType
import com.akergez.superreader2000.shared.formats.abstractions.IRenderHelper
import com.akergez.superreader2000.shared.formats.pdf.TOCEntry
import com.akergez.superreader2000.shared.ui.DjvuBookContent
import kotlinx.coroutines.flow.StateFlow

actual class DJVURenderHelper :
    IRenderHelper<DjvuBookContent> {
    override fun openPage(
        pageIndex: Int,
        applyInversion: Boolean
    ): StateFlow<DjvuBookContent> {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }

    actual override fun getBookType(): FormatType {
        TODO("Not yet implemented")
    }

    actual override fun getTableOfContents(): StateFlow<List<TOCEntry>> {
        TODO("Not yet implemented")
    }
}