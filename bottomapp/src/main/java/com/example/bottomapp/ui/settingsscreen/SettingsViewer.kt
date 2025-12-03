@file:OptIn(ExperimentalFoundationApi::class)

package com.example.bottomapp.ui.settingsscreen

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.navigation.NavController
import com.example.bottomapp.R
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.ui.homescreen.components.IconScrollSelector
import com.example.bottomapp.ui.mainscreen.BottomNavigationBar
import com.example.bottomapp.ui.mainscreen.Screen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
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

    Scaffold(bottomBar = { BottomNavigationBar(navController, Screen.SETTINGS_SCREEN) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = 20.dp,
                    end = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(4.dp))
            SettingsGroup(getString(LocalContext.current, R.string.settings_group_general))
            ThemeSelector(
                getString(LocalContext.current, R.string.app_theme),
                mapOf(
                    AppThemeMode.System to getString(LocalContext.current, R.string.app_theme_system),
                    AppThemeMode.Light to getString(LocalContext.current, R.string.app_theme_light),
                    AppThemeMode.Dark to getString(LocalContext.current, R.string.app_theme_dark)
                ),
                settings.appThemeMode
            ) { mode ->
                settingsViewModel.updateAppThemeMode(mode)
            }

            SettingsOption(getString(LocalContext.current, R.string.title_storage_address))
            Row(Modifier
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
                    contentDescription = getString(LocalContext.current, R.string.title_storage_address),
                    Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = settings.storageAddress,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(16.dp))

            SettingsGroup(getString(LocalContext.current, R.string.settings_group_reading))
            ThemeSelector(
                getString(LocalContext.current, R.string.documents_theme),
                mapOf(
                    DocThemeMode.AsIs to getString(LocalContext.current, R.string.documents_theme_as_is),
                    DocThemeMode.Invert to getString(LocalContext.current, R.string.documents_theme_invert),
                ),
                settings.docThemeMode
            ) { mode ->
                settingsViewModel.updateDocThemeMode(mode)
            }

            SettingsOption(getString(LocalContext.current, R.string.scrolling_mode))
            IconScrollSelector(onSelect = {settingsViewModel.updateFlipMode(it)}, selected = settings.flipMode)
        }
    }
}

@Composable
fun SettingsGroup(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .padding(vertical = 10.dp)
    )
}

@Composable
fun SettingsOption(name: String) {
    Text(name, style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 10.dp) )
}

@Composable
fun <TEnum> ThemeSelector(
    title: String,
    values: Map<TEnum, String>,
    selectedTheme: TEnum,
    onThemeChange: (TEnum) -> Unit
) {
    SettingsOption(title)

    val options = values.values.toList()
    var selectedIndex by remember { mutableIntStateOf(options.indexOf(values[selectedTheme])) }

    SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(bottom = 15.dp)){
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = {
                    selectedIndex = index
                    val selectedValue = values.keys.toList()[index]
                    onThemeChange(selectedValue)
                },
                selected = index == selectedIndex,
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun PreviewThemeSelector() {
    var selectedTheme by remember { mutableStateOf(AppThemeMode.System) }

    ThemeSelector(
        "what",
        values =
        mapOf(
            AppThemeMode.Light to "app_theme_light",
            AppThemeMode.Dark to "app_theme_dark",
            AppThemeMode.System to "app_theme_system"
        ),
        selectedTheme = selectedTheme,
        onThemeChange = { selectedTheme = it }
    )
}

