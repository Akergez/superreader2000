package com.akergez.superreader2000.shared

import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.backend.settings.ReaderFont
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSettingsManager(
    storagePath: String? = null,
    appTheme: AppThemeMode = AppThemeMode.System,
    docTheme: DocThemeMode = DocThemeMode.AsIs,
    flipMode: FlipMode = FlipMode.PagingCurl,
) : ISettingsManager {
    private val _appThemeModeFlow = MutableStateFlow(appTheme)
    override val appThemeModeFlow: StateFlow<AppThemeMode> = _appThemeModeFlow

    private var _appTheme = appTheme
    private var _docTheme = docTheme
    private var _textDocTheme = docTheme
    private var _fixedDocTheme = docTheme
    private var _flipMode = flipMode
    private var _storagePath = storagePath
    private var _readerFont = ReaderFont.Literata
    private var _readerFontSize = 18f

    override fun getAppTheme(defaultTheme: AppThemeMode) = _appTheme
    override fun setAppTheme(theme: AppThemeMode) {
        _appTheme = theme
        _appThemeModeFlow.value = theme
    }

    override fun getDocumentsTheme(defaultTheme: DocThemeMode) = _docTheme
    override fun setDocumentsTheme(theme: DocThemeMode) { _docTheme = theme }
    override fun getTextDocumentsTheme(defaultTheme: DocThemeMode) = _textDocTheme
    override fun setTextDocumentsTheme(theme: DocThemeMode) { _textDocTheme = theme }
    override fun getFixedDocumentsTheme(defaultTheme: DocThemeMode) = _fixedDocTheme
    override fun setFixedDocumentsTheme(theme: DocThemeMode) {
        _fixedDocTheme = theme
        _docTheme = theme
    }

    override fun getFlipMode() = _flipMode
    override fun setFlipMode(mode: FlipMode) { _flipMode = mode }
    override fun getReaderFont() = _readerFont
    override fun setReaderFont(font: ReaderFont) { _readerFont = font }
    override fun getReaderFontSize() = _readerFontSize
    override fun setReaderFontSize(size: Float) { _readerFontSize = size }

    override fun getStoragePath() = _storagePath
    override fun setStoragePath(path: String) { _storagePath = path }
    override fun clearStoragePath() { _storagePath = null }
}
