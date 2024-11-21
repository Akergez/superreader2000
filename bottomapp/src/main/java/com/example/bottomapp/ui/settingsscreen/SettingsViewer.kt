@file:OptIn(ExperimentalFoundationApi::class)

package com.example.bottomapp.ui.settingsscreen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bottomapp.BottomNavigationBar
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.ui.readerscreens.common.FlipMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = koinViewModel(),
    navController: NavController
) {
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
                    settingsViewModel.updateStorageAddress(uri)
                }
            }
        }


    Scaffold(bottomBar = { BottomNavigationBar(navController) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingRow(
                "App Theme",
                mapOf(
                    AppThemeMode.Light to "Light",
                    AppThemeMode.Dark to "Dark",
                    AppThemeMode.System to "System"
                ),
                settings.appThemeMode
            ) { mode ->
                settingsViewModel.updateAppThemeMode(mode)
            }

            SettingRow(
                "Documents theme",
                mapOf(
                    DocThemeMode.AsIs to "As is",
                    DocThemeMode.Invert to "Invert",
                ),
                settings.docThemeMode
            ) { mode ->
                settingsViewModel.updateDocThemeMode(mode)
            }

            Row(Modifier.padding(32.dp)) {

            }

            SettingRow(
                "Paging mode",
                mapOf(
                    FlipMode.PagingCurl to "Classic",
                    FlipMode.VerticalScroll to "Vertical scroll",
                ),
                settings.flipMode
            ) { mode ->
                settingsViewModel.updateFlipMode(mode)
            }

            Row(Modifier.padding(32.dp)) {
            }

            Text("Storage address", style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = settings.storageAddress,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                directoryLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
                            }
                        )
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun <TEnum> SettingRow(
    title: String,
    values: Map<TEnum, String>,
    selectedValue: TEnum,
    onValueChange: (TEnum) -> Unit
) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Row(Modifier.padding(vertical = 8.dp)) {
        values.forEach { value ->
            RadioButton(
                selected = selectedValue == value.key,
                onClick = { onValueChange(value.key) })
            Text(text = value.value,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .clickable { onValueChange(value.key) }
                    .padding(start = 8.dp))
        }
    }
}


