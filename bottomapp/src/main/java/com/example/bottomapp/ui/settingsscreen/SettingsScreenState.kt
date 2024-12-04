package com.example.bottomapp.ui.settingsscreen

import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.ui.readerscreens.common.FlipMode

data class SettingsScreenState(
    val storageAddress: String = "",
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
    val flipMode: FlipMode = FlipMode.PagingCurl,
    val appThemeMode: AppThemeMode = AppThemeMode.System
)