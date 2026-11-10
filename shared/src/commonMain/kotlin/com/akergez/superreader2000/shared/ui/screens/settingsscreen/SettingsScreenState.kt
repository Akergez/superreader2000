package com.akergez.superreader2000.shared.ui.screens.settingsscreen

import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.DEFAULT_READER_FONT_SIZE
import com.akergez.superreader2000.shared.backend.settings.ReaderFont
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode

data class SettingsScreenState(
    val storageAddress: String = "",
    val textDocThemeMode: DocThemeMode = DocThemeMode.AsIs,
    val fixedDocThemeMode: DocThemeMode = DocThemeMode.AsIs,
    val flipMode: FlipMode = FlipMode.PagingCurl,
    val appThemeMode: AppThemeMode = AppThemeMode.System,
    val readerFont: ReaderFont = ReaderFont.Literata,
    val readerFontSize: Float = DEFAULT_READER_FONT_SIZE,
    val compatibilityTextRenderer: Boolean = false
) {
    val docThemeMode: DocThemeMode get() = fixedDocThemeMode
}
