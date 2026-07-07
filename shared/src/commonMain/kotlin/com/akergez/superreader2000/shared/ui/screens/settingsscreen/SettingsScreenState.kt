package com.akergez.superreader2000.shared.ui.screens.settingsscreen

import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import com.akergez.tresse.kmp.TresseConfig

enum class SyncUiStatus { Idle, Running, ForcePullRunning, Success, Error, Saved, Imported, ValidationError, ExportWarning, ImportError }

data class SettingsScreenState(
    val storageAddress: String = "",
    val docThemeMode: DocThemeMode = DocThemeMode.AsIs,
    val flipMode: FlipMode = FlipMode.PagingCurl,
    val appThemeMode: AppThemeMode = AppThemeMode.System,
    val s3Config: TresseConfig = TresseConfig(storageType = "s3", repoId = "superreader"),
    val syncStatus: SyncUiStatus = SyncUiStatus.Idle,
    val pulled: Int = 0,
    val pushed: Int = 0,
    val syncErrorDetail: String = "",
    val configShareString: String = ""
)
