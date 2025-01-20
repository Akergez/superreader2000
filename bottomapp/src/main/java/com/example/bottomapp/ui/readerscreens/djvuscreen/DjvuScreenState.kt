package com.example.bottomapp.ui.readerscreens.djvuscreen

import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.ui.readerscreens.common.FlipMode
import com.example.bottomapp.ui.readerscreens.common.ReaderMode

data class DjvuScreenState(
    val currentPage: Int = 0,
    val mode: ReaderMode = ReaderMode.Reading,
    val curlMode: FlipMode = FlipMode.PagingCurl,
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
)
