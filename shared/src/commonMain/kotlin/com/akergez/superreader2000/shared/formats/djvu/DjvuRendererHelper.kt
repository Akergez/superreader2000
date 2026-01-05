package com.akergez.superreader2000.shared.formats.djvu

import com.akergez.superreader2000.shared.formats.abstractions.FormatType
import com.akergez.superreader2000.shared.formats.abstractions.IRenderHelper
import com.akergez.superreader2000.shared.formats.pdf.TOCEntry
import com.akergez.superreader2000.shared.ui.DjvuBookContent
import kotlinx.coroutines.flow.StateFlow

expect class DJVURenderHelper : IRenderHelper<DjvuBookContent> {
    override fun close()
    override fun getBookType(): FormatType
    override fun getTableOfContents(): StateFlow<List<TOCEntry>>
}