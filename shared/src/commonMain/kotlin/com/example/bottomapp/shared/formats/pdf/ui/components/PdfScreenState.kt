package com.example.bottomapp.shared.formats.pdf.ui.components

import com.example.bottomapp.shared.backend.settings.DocThemeMode
import com.example.bottomapp.shared.ui.screens.readerscreens.common.FlipMode
import com.example.bottomapp.shared.ui.screens.readerscreens.common.ReaderMode

data class PdfScreenState(
    val currentPage: Int = 0,
    val mode: ReaderMode = ReaderMode.Reading,
    val curlMode: FlipMode = FlipMode.PagingCurl,
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
)