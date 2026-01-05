package com.akergez.superreader2000.shared.formats.djvu.ui

import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.ReaderMode

data class DjvuScreenState(
    val currentPage: Int = 0,
    val mode: ReaderMode = ReaderMode.Reading,
    val curlMode: FlipMode = FlipMode.PagingCurl,
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
)
