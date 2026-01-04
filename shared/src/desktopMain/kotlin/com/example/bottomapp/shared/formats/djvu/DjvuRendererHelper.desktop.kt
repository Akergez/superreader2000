package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.formats.abstractions.IRenderHelper
import com.example.bottomapp.shared.formats.pdf.TOCEntry
import com.example.bottomapp.shared.ui.DjvuBookContent
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