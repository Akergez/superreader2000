@file:OptIn(ExperimentalFoundationApi::class)

package com.akergez.superreader2000.shared.ui.screens.settingsscreen

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.ui.screens.settingsscreen.components.IconScrollSelector
import com.akergez.superreader2000.shared.ui.screens.mainscreen.rememberStorageDirectoryPicker
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.app_theme
import superreader2000.shared.generated.resources.app_theme_dark
import superreader2000.shared.generated.resources.app_theme_light
import superreader2000.shared.generated.resources.app_theme_system
import superreader2000.shared.generated.resources.documents_theme
import superreader2000.shared.generated.resources.documents_theme_as_is
import superreader2000.shared.generated.resources.documents_theme_invert
import superreader2000.shared.generated.resources.error_book_already_exists
import superreader2000.shared.generated.resources.scrolling_mode
import superreader2000.shared.generated.resources.settings_group_general
import superreader2000.shared.generated.resources.settings_group_reading
import superreader2000.shared.generated.resources.title_storage_address

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(navController: NavController, bottomBar: @Composable () -> Unit = {}) {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settings by settingsViewModel.settingsState.collectAsState()
    val directoryLauncher = rememberStorageDirectoryPicker(
        onDirectorySelected = { uri ->
            settingsViewModel.updateStorageAddress(uri)
        },
        onPermissionGranted = {}
    )

    @Suppress("UNUSED_VARIABLE", "UNUSED_EXPRESSION")
    stringResource(Res.string.error_book_already_exists)

    Scaffold(bottomBar = bottomBar) { innerPadding ->
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
                            directoryLauncher.launch()
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
    Text(
        name, style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

@Composable
@Suppress("UNUSED")
fun <TEnum> ThemeSelector(
    title: String,
    values: Map<TEnum, String>,
    selectedTheme: TEnum,
    onThemeChange: (TEnum) -> Unit
) {
    SettingsOption(title)

    val options = values.values.toList()
    val selectedIndex = remember { mutableIntStateOf(options.indexOf(values[selectedTheme])) }

    SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(bottom = 15.dp)) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = {
                    selectedIndex.intValue = index
                    val selectedValue = values.keys.toList()[index]
                    onThemeChange(selectedValue)
                },
                selected = index == selectedIndex.intValue,
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
