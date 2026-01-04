package com.example.bottomapp.shared.formats.djvu

import com.example.bottomapp.shared.formats.abstractions.FormatType
import com.example.bottomapp.shared.formats.abstractions.IRenderHelper
import com.example.bottomapp.shared.formats.pdf.TOCEntry
import com.example.bottomapp.shared.ui.DjvuBookContent
import kotlinx.coroutines.flow.StateFlow

expect class DJVURenderHelper : IRenderHelper<DjvuBookContent> {
    override fun close()
    override fun getBookType(): FormatType
    override fun getTableOfContents(): StateFlow<List<TOCEntry>>
}