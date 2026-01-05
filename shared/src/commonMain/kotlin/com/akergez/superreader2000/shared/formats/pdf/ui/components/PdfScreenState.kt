package com.akergez.superreader2000.shared.formats.pdf.ui.components

import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.ReaderMode

data class PdfScreenState(
    val currentPage: Int = 0,
    val mode: ReaderMode = ReaderMode.Reading,
    val curlMode: FlipMode = FlipMode.PagingCurl,
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
)