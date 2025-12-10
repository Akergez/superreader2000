package com.example.bottomapp.shared.ui.screens.settingsscreen

import com.example.bottomapp.shared.backend.settings.AppThemeMode
import com.example.bottomapp.shared.backend.settings.DocThemeMode
import com.example.bottomapp.shared.ui.screens.readerscreens.common.FlipMode

data class SettingsScreenState(
    val storageAddress: String = "",
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
    val flipMode: FlipMode = FlipMode.PagingCurl,
    val appThemeMode: AppThemeMode = AppThemeMode.System
)