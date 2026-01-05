package com.akergez.superreader2000.shared.ui.screens.settingsscreen

import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode

data class SettingsScreenState(
    val storageAddress: String = "",
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
    val flipMode: FlipMode = FlipMode.PagingCurl,
    val appThemeMode: AppThemeMode = AppThemeMode.System
)