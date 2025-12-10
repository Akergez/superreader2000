package com.example.bottomapp.shared.ui.screens.settingsscreen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavController
import com.example.bottomapp.shared.backend.settings.AppThemeMode
import com.example.bottomapp.shared.backend.settings.DocThemeMode
import com.example.bottomapp.shared.ui.components.BottomNavigationBar
import com.example.bottomapp.shared.ui.navigation.Screen
import com.example.bottomapp.shared.ui.screens.homescreen.components.IconScrollSelector
import org.koin.compose.viewmodel.koinViewModel
import pdfreaderjetpackcompose.shared.generated.resources.Res
import pdfreaderjetpackcompose.shared.generated.resources.app_theme
import pdfreaderjetpackcompose.shared.generated.resources.app_theme_dark
import pdfreaderjetpackcompose.shared.generated.resources.app_theme_light
import pdfreaderjetpackcompose.shared.generated.resources.app_theme_system
import pdfreaderjetpackcompose.shared.generated.resources.documents_theme
import pdfreaderjetpackcompose.shared.generated.resources.documents_theme_as_is
import pdfreaderjetpackcompose.shared.generated.resources.documents_theme_invert
import pdfreaderjetpackcompose.shared.generated.resources.error_book_already_exists
import pdfreaderjetpackcompose.shared.generated.resources.scrolling_mode
import pdfreaderjetpackcompose.shared.generated.resources.settings_group_general
import pdfreaderjetpackcompose.shared.generated.resources.settings_group_reading
import pdfreaderjetpackcompose.shared.generated.resources.title_storage_address

@OptIn(markerClass = [ExperimentalFoundationApi::class])
@Composable
actual fun SettingsScreen(navController: Any) {
    val actualNavController = navController as NavController
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()
    val directoryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    settingsViewModel.updateStorageAddress(uri.toString())
                }
            }
        }

    val neededResourceDONOTDELETE = stringResource(Res.string.error_book_already_exists)

    Scaffold(bottomBar = {
        BottomNavigationBar(
            actualNavController,
            Screen.SETTINGS_SCREEN
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = 20.dp,
                    end = 20.dp
                )
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(4.dp))
            SettingsGroup(stringResource(Res.string.settings_group_general))
            ThemeSelector(
                stringResource(Res.string.app_theme),
                mapOf(
                    AppThemeMode.System to stringResource(Res.string.app_theme_system),
                    AppThemeMode.Light to stringResource(Res.string.app_theme_light),
                    AppThemeMode.Dark to stringResource(Res.string.app_theme_dark)
                ),
                settings.appThemeMode
            ) { mode ->
                settingsViewModel.updateAppThemeMode(mode)
            }

            SettingsOption(stringResource(Res.string.title_storage_address))
            Row(
                Modifier
                    .padding(vertical = 8.dp)
                    .height(26.dp)
                    .combinedClickable(
                        onClick = {
                            directoryLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(Res.string.title_storage_address),
                    Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = settings.storageAddress,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(16.dp))

            SettingsGroup(stringResource(Res.string.settings_group_reading))
            ThemeSelector(
                stringResource(Res.string.documents_theme),
                mapOf(
                    DocThemeMode.AsIs to stringResource(Res.string.documents_theme_as_is),
                    DocThemeMode.Invert to stringResource(Res.string.documents_theme_invert),
                ),
                settings.docThemeMode
            ) { mode ->
                settingsViewModel.updateDocThemeMode(mode)
            }

            SettingsOption(stringResource(Res.string.scrolling_mode))
            IconScrollSelector(
                onSelect = { settingsViewModel.updateFlipMode(it) },
                selected = settings.flipMode
            )
        }
    }
}


