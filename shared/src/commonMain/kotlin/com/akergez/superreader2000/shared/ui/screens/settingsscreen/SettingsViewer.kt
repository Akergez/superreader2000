@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.MAX_READER_FONT_SIZE
import com.akergez.superreader2000.shared.backend.settings.MIN_READER_FONT_SIZE
import com.akergez.superreader2000.shared.backend.settings.ReaderFont
import com.akergez.superreader2000.shared.constants.Route
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
import superreader2000.shared.generated.resources.documents_theme_material_you
import superreader2000.shared.generated.resources.documents_theme_soft_dark
import superreader2000.shared.generated.resources.documents_theme_soft_light
import superreader2000.shared.generated.resources.documents_theme_black_on_white
import superreader2000.shared.generated.resources.documents_theme_white_on_black
import superreader2000.shared.generated.resources.text_documents_theme
import superreader2000.shared.generated.resources.fixed_documents_theme
import superreader2000.shared.generated.resources.error_book_already_exists
import superreader2000.shared.generated.resources.scrolling_mode
import superreader2000.shared.generated.resources.settings_group_general
import superreader2000.shared.generated.resources.settings_group_reading
import superreader2000.shared.generated.resources.title_storage_address
import superreader2000.shared.generated.resources.s3_sync_group
import superreader2000.shared.generated.resources.sync_settings_description
import superreader2000.shared.generated.resources.s3_endpoint
import superreader2000.shared.generated.resources.s3_region
import superreader2000.shared.generated.resources.s3_bucket
import superreader2000.shared.generated.resources.s3_prefix
import superreader2000.shared.generated.resources.s3_access_key_id
import superreader2000.shared.generated.resources.s3_secret_access_key
import superreader2000.shared.generated.resources.s3_encryption_key
import superreader2000.shared.generated.resources.s3_config_string
import superreader2000.shared.generated.resources.s3_export_secret_warning
import superreader2000.shared.generated.resources.s3_save
import superreader2000.shared.generated.resources.s3_sync
import superreader2000.shared.generated.resources.s3_force_pull
import superreader2000.shared.generated.resources.s3_export
import superreader2000.shared.generated.resources.s3_import
import superreader2000.shared.generated.resources.s3_saved
import superreader2000.shared.generated.resources.s3_imported
import superreader2000.shared.generated.resources.s3_validation_error
import superreader2000.shared.generated.resources.s3_sync_running
import superreader2000.shared.generated.resources.s3_force_pull_running
import superreader2000.shared.generated.resources.s3_sync_success
import superreader2000.shared.generated.resources.s3_sync_error
import superreader2000.shared.generated.resources.s3_import_error
import superreader2000.shared.generated.resources.s3_generate_key
import superreader2000.shared.generated.resources.s3_wipe_chunks
import superreader2000.shared.generated.resources.s3_wipe_chunks_hint
import superreader2000.shared.generated.resources.s3_wipe_chunks_confirm
import superreader2000.shared.generated.resources.s3_wipe_chunks_action
import superreader2000.shared.generated.resources.s3_wiping
import superreader2000.shared.generated.resources.s3_wiped
import superreader2000.shared.generated.resources.cancel
import superreader2000.shared.generated.resources.reader_font
import superreader2000.shared.generated.resources.reader_font_sans_serif
import superreader2000.shared.generated.resources.reader_font_serif
import superreader2000.shared.generated.resources.reader_font_size
import superreader2000.shared.generated.resources.compatibility_text_renderer
import superreader2000.shared.generated.resources.compatibility_text_renderer_description
import superreader2000.shared.generated.resources.tresse_sync_title
import superreader2000.shared.generated.resources.sync_settings_title

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
                stringResource(Res.string.text_documents_theme),
                mapOf(
                    DocThemeMode.AsIs to stringResource(Res.string.documents_theme_black_on_white),
                    DocThemeMode.Invert to stringResource(Res.string.documents_theme_white_on_black),
                    DocThemeMode.SoftLight to stringResource(Res.string.documents_theme_soft_light),
                    DocThemeMode.SoftDark to stringResource(Res.string.documents_theme_soft_dark),
                    DocThemeMode.MaterialYou to stringResource(Res.string.documents_theme_material_you),
                ),
                settings.textDocThemeMode
            ) { mode ->
                settingsViewModel.updateTextDocThemeMode(mode)
            }

            ThemeSelector(
                stringResource(Res.string.fixed_documents_theme),
                mapOf(
                    DocThemeMode.AsIs to stringResource(Res.string.documents_theme_black_on_white),
                    DocThemeMode.Invert to stringResource(Res.string.documents_theme_white_on_black),
                ),
                settings.fixedDocThemeMode,
                settingsViewModel::updateFixedDocThemeMode,
            )

            ThemeSelector(
                stringResource(Res.string.reader_font),
                mapOf(
                    ReaderFont.Literata to stringResource(Res.string.reader_font_serif),
                    ReaderFont.Roboto to stringResource(Res.string.reader_font_sans_serif),
                ),
                settings.readerFont,
                settingsViewModel::updateReaderFont,
            )
            Text(stringResource(Res.string.reader_font_size, settings.readerFontSize.toInt()))
            Slider(
                value = settings.readerFontSize,
                onValueChange = settingsViewModel::updateReaderFontSize,
                valueRange = MIN_READER_FONT_SIZE..MAX_READER_FONT_SIZE,
                steps = (MAX_READER_FONT_SIZE - MIN_READER_FONT_SIZE).toInt() - 1,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.compatibility_text_renderer))
                    Text(
                        stringResource(Res.string.compatibility_text_renderer_description),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = settings.compatibilityTextRenderer,
                    onCheckedChange = settingsViewModel::updateCompatibilityTextRenderer,
                )
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
fun SyncSettingsScreen(bottomBar: @Composable () -> Unit = {}) {
    val viewModel: SettingsViewModel = koinViewModel()
    val settings by viewModel.settingsState.collectAsState()
    val busy = settings.syncStatus in setOf(
        SyncUiStatus.Running, SyncUiStatus.ForcePullRunning, SyncUiStatus.Wiping
    )
    var confirmWipe by remember { mutableStateOf(false) }

    Scaffold(bottomBar = bottomBar) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsGroup(stringResource(Res.string.tresse_sync_title))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(viewModel::saveS3Config, enabled = !busy) { Text(stringResource(Res.string.s3_save)) }
                Spacer(Modifier.width(8.dp))
                Button(viewModel::synchronize, enabled = !busy) { Text(stringResource(Res.string.s3_sync)) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(viewModel::forcePull, enabled = !busy) { Text(stringResource(Res.string.s3_force_pull)) }
                if (busy) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            val message = when (settings.syncStatus) {
                SyncUiStatus.Idle -> ""
                SyncUiStatus.Running -> stringResource(Res.string.s3_sync_running)
                SyncUiStatus.ForcePullRunning -> stringResource(Res.string.s3_force_pull_running)
                SyncUiStatus.Success -> stringResource(Res.string.s3_sync_success, settings.pulled, settings.pushed)
                SyncUiStatus.Error -> stringResource(Res.string.s3_sync_error, settings.syncErrorDetail)
                SyncUiStatus.Saved -> stringResource(Res.string.s3_saved)
                SyncUiStatus.Imported -> stringResource(Res.string.s3_imported)
                SyncUiStatus.ValidationError -> stringResource(Res.string.s3_validation_error)
                SyncUiStatus.ExportWarning -> stringResource(Res.string.s3_export_secret_warning)
                SyncUiStatus.ImportError -> stringResource(Res.string.s3_import_error)
                SyncUiStatus.Wiping -> stringResource(Res.string.s3_wiping)
                SyncUiStatus.Wiped -> stringResource(Res.string.s3_wiped)
            }
            if (message.isNotBlank()) {
                Text(
                    message,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = if (settings.syncStatus in setOf(SyncUiStatus.Error, SyncUiStatus.ValidationError, SyncUiStatus.ImportError, SyncUiStatus.ExportWarning)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            SettingsOption(stringResource(Res.string.sync_settings_title))
            S3Field(stringResource(Res.string.s3_endpoint), settings.s3Config.s3Endpoint) {
                viewModel.updateS3Config(settings.s3Config.copy(s3Endpoint = it))
            }
            S3Field(stringResource(Res.string.s3_region), settings.s3Config.s3Region) {
                viewModel.updateS3Config(settings.s3Config.copy(s3Region = it))
            }
            S3Field(stringResource(Res.string.s3_bucket), settings.s3Config.s3Bucket) {
                viewModel.updateS3Config(settings.s3Config.copy(s3Bucket = it))
            }
            S3Field(stringResource(Res.string.s3_prefix), settings.s3Config.repoId) {
                viewModel.updateS3Config(settings.s3Config.copy(repoId = it))
            }
            S3Field(stringResource(Res.string.s3_access_key_id), settings.s3Config.s3AccessKeyId) {
                viewModel.updateS3Config(settings.s3Config.copy(s3AccessKeyId = it))
            }
            S3Field(stringResource(Res.string.s3_secret_access_key), settings.s3Config.s3SecretAccessKey, true) {
                viewModel.updateS3Config(settings.s3Config.copy(s3SecretAccessKey = it))
            }
            S3Field(stringResource(Res.string.s3_encryption_key), settings.s3Config.encryptionKey, true) {
                viewModel.updateS3Config(settings.s3Config.copy(encryptionKey = it))
            }
            OutlinedButton(viewModel::generateEncryptionKey, Modifier.padding(top = 6.dp)) {
                Text(stringResource(Res.string.s3_generate_key))
            }
            OutlinedTextField(
                value = settings.configShareString,
                onValueChange = viewModel::updateConfigShareString,
                label = { Text(stringResource(Res.string.s3_config_string)) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                minLines = 2,
                maxLines = 4,
            )
            Text(
                stringResource(Res.string.s3_export_secret_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedButton(viewModel::exportS3Config) { Text(stringResource(Res.string.s3_export)) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(viewModel::importS3Config) { Text(stringResource(Res.string.s3_import)) }
            }
            Text(
                stringResource(Res.string.s3_wipe_chunks_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            OutlinedButton(
                onClick = { confirmWipe = true },
                enabled = !busy,
                modifier = Modifier.padding(top = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(stringResource(Res.string.s3_wipe_chunks)) }
            if (confirmWipe) {
                AlertDialog(
                    onDismissRequest = { confirmWipe = false },
                    title = { Text(stringResource(Res.string.s3_wipe_chunks)) },
                    text = { Text(stringResource(Res.string.s3_wipe_chunks_confirm)) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmWipe = false
                            viewModel.wipeLocalChunks()
                        }) { Text(stringResource(Res.string.s3_wipe_chunks_action)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmWipe = false }) {
                            Text(stringResource(Res.string.cancel))
                        }
                    },
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun S3Field(label: String, value: String, secret: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
    )
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
