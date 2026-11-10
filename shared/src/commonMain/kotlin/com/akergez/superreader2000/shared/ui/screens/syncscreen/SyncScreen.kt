package com.akergez.superreader2000.shared.ui.screens.syncscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.akergez.superreader2000.shared.sync.ServerConnectFailure
import com.akergez.superreader2000.shared.sync.ServerConnectReturns
import com.akergez.superreader2000.shared.ui.screens.settingsscreen.SettingsGroup
import com.akergez.tresse.kmp.TresseConfig
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.s3_import
import superreader2000.shared.generated.resources.cancel
import superreader2000.shared.generated.resources.close
import superreader2000.shared.generated.resources.ok
import superreader2000.shared.generated.resources.s3_access_key_id
import superreader2000.shared.generated.resources.s3_bucket
import superreader2000.shared.generated.resources.s3_encryption_key
import superreader2000.shared.generated.resources.s3_endpoint
import superreader2000.shared.generated.resources.s3_force_pull
import superreader2000.shared.generated.resources.s3_force_pull_running
import superreader2000.shared.generated.resources.s3_generate_key
import superreader2000.shared.generated.resources.s3_imported
import superreader2000.shared.generated.resources.s3_prefix
import superreader2000.shared.generated.resources.s3_region
import superreader2000.shared.generated.resources.s3_save
import superreader2000.shared.generated.resources.s3_saved
import superreader2000.shared.generated.resources.s3_secret_access_key
import superreader2000.shared.generated.resources.s3_sync
import superreader2000.shared.generated.resources.s3_sync_error
import superreader2000.shared.generated.resources.s3_sync_running
import superreader2000.shared.generated.resources.s3_sync_success
import superreader2000.shared.generated.resources.s3_validation_error
import superreader2000.shared.generated.resources.s3_wipe_chunks
import superreader2000.shared.generated.resources.s3_wipe_chunks_action
import superreader2000.shared.generated.resources.s3_wipe_chunks_confirm
import superreader2000.shared.generated.resources.s3_wipe_chunks_hint
import superreader2000.shared.generated.resources.s3_wiped
import superreader2000.shared.generated.resources.s3_wiping
import superreader2000.shared.generated.resources.storage_http
import superreader2000.shared.generated.resources.storage_s3
import superreader2000.shared.generated.resources.sync_config_import_error
import superreader2000.shared.generated.resources.sync_connect_action
import superreader2000.shared.generated.resources.sync_connect_begin_failed
import superreader2000.shared.generated.resources.sync_connect_confirm_body
import superreader2000.shared.generated.resources.sync_connect_confirm_title
import superreader2000.shared.generated.resources.sync_connect_error_link
import superreader2000.shared.generated.resources.sync_connect_error_no_config
import superreader2000.shared.generated.resources.sync_connect_error_no_key
import superreader2000.shared.generated.resources.sync_connect_error_other_server
import superreader2000.shared.generated.resources.sync_connect_key_generated
import superreader2000.shared.generated.resources.sync_details_hint
import superreader2000.shared.generated.resources.sync_export_copied
import superreader2000.shared.generated.resources.sync_export_copy
import superreader2000.shared.generated.resources.sync_export_description
import superreader2000.shared.generated.resources.sync_export_string
import superreader2000.shared.generated.resources.sync_repo_id
import superreader2000.shared.generated.resources.sync_server_field
import superreader2000.shared.generated.resources.sync_server_offer_button
import superreader2000.shared.generated.resources.sync_server_offer_description
import superreader2000.shared.generated.resources.sync_server_url
import superreader2000.shared.generated.resources.sync_string_description
import superreader2000.shared.generated.resources.sync_string_field
import superreader2000.shared.generated.resources.sync_tab_details
import superreader2000.shared.generated.resources.sync_tab_main
import superreader2000.shared.generated.resources.sync_token
import superreader2000.shared.generated.resources.tresse_sync_title

/**
 * Экран синхронизации. Статус и запуск синка — над вкладками; «Основное» — вход через
 * сервер Tresse (предпочтительный путь), вставка строки подключения и её экспорт;
 * «Подробно» — все поля удалённого хранилища: сервер Tresse или свой S3.
 */
@Composable
fun SyncScreen(bottomBar: @Composable () -> Unit = {}) {
    val viewModel: SyncViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }

    // Возврат со страницы сервера забирает только показанный экран: NavHost компонует
    // лишь текущий, так что диалог не всплывёт на невидимой копии.
    val returned by ServerConnectReturns.pending.collectAsState()
    LaunchedEffect(returned) {
        if (returned != null) viewModel.acceptServerReturn()
    }

    Scaffold(bottomBar = bottomBar) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsGroup(stringResource(Res.string.tresse_sync_title))
            SyncStatus(state, onSync = viewModel::synchronize, onForcePull = viewModel::forcePull)

            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(Res.string.sync_tab_main)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(Res.string.sync_tab_details)) })
            }
            when (tab) {
                0 -> SyncMainTab(state, viewModel)
                else -> SyncDetailsTab(state, viewModel)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    state.connectEvent?.let { event ->
        ServerConnectDialog(event, onResolve = viewModel::resolveServerConnect)
    }
}

@Composable
private fun SyncStatus(state: SyncScreenState, onSync: () -> Unit, onForcePull: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Button(onSync, enabled = !state.busy) { Text(stringResource(Res.string.s3_sync)) }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onForcePull, enabled = !state.busy) { Text(stringResource(Res.string.s3_force_pull)) }
        if (state.busy) {
            Spacer(Modifier.width(12.dp))
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
    val message = when (state.status) {
        SyncUiStatus.Idle -> ""
        SyncUiStatus.Running -> stringResource(Res.string.s3_sync_running)
        SyncUiStatus.ForcePullRunning -> stringResource(Res.string.s3_force_pull_running)
        SyncUiStatus.Success -> stringResource(Res.string.s3_sync_success, state.pulled, state.pushed)
        SyncUiStatus.Error -> stringResource(Res.string.s3_sync_error, state.errorDetail)
        SyncUiStatus.Saved -> stringResource(Res.string.s3_saved)
        SyncUiStatus.Imported -> stringResource(Res.string.s3_imported)
        SyncUiStatus.ValidationError -> stringResource(Res.string.s3_validation_error)
        SyncUiStatus.Wiping -> stringResource(Res.string.s3_wiping)
        SyncUiStatus.Wiped -> stringResource(Res.string.s3_wiped)
    }
    if (message.isNotBlank()) {
        if (state.status in setOf(SyncUiStatus.Error, SyncUiStatus.ValidationError)) ErrorText(message) else Hint(message)
    }
}

/** «Основное»: вход через сервер, строка подключения, экспорт. */
@Composable
private fun ColumnScope.SyncMainTab(state: SyncScreenState, viewModel: SyncViewModel) {
    SyncField(stringResource(Res.string.sync_server_field), state.serverAddress, placeholder = "https://…") {
        viewModel.updateServerAddress(it)
    }
    Button(
        onClick = viewModel::beginServerConnect,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.serverAddress.isNotBlank(),
    ) { Text(stringResource(Res.string.sync_server_offer_button)) }
    Hint(stringResource(Res.string.sync_server_offer_description))
    if (state.connectBeginFailed) ErrorText(stringResource(Res.string.sync_connect_begin_failed))

    HorizontalDivider()

    OutlinedTextField(
        value = state.connectionString,
        onValueChange = viewModel::updateConnectionString,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.sync_string_field)) },
        placeholder = { Text("tresse://http?url=…") },
        minLines = 2,
    )
    Hint(stringResource(Res.string.sync_string_description))
    state.importFailure?.let { ErrorText(connectFailureText(it)) }
    TextButton(
        onClick = viewModel::importConnectionString,
        enabled = state.connectionString.isNotBlank(),
        modifier = Modifier.align(Alignment.End),
    ) { Text(stringResource(Res.string.s3_import)) }

    // Экспорт — когда настроено всё, каким бы ни был ремоут: строка нужна следующему
    // устройству, и ключ, созданный здесь, иначе негде сохранить.
    state.exportString?.let { exported ->
        HorizontalDivider()
        ExportConfigString(exported)
    }
}

/** «Подробно»: поля хранилища — сервер Tresse (по умолчанию) или свой S3. */
@Composable
private fun ColumnScope.SyncDetailsTab(state: SyncScreenState, viewModel: SyncViewModel) {
    val config = state.config
    fun update(transform: TresseConfig.() -> TresseConfig) = viewModel.updateConfig(config.transform())

    Hint(stringResource(Res.string.sync_details_hint))
    val storageTypes = listOf("http" to stringResource(Res.string.storage_http), "s3" to stringResource(Res.string.storage_s3))
    val selectedType = if (config.storageType == "s3") "s3" else "http"
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        storageTypes.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = selectedType == type,
                onClick = { update { copy(storageType = type) } },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = storageTypes.size),
                label = { Text(label) },
            )
        }
    }

    if (selectedType == "http") {
        SyncField(stringResource(Res.string.sync_server_url), config.serverUrl, placeholder = "https://…") {
            update { copy(serverUrl = it) }
        }
        SyncField(stringResource(Res.string.sync_repo_id), config.repoId) { update { copy(repoId = it) } }
        SyncField(stringResource(Res.string.sync_token), config.token, secret = true) { update { copy(token = it) } }
    } else {
        SyncField(stringResource(Res.string.s3_endpoint), config.s3Endpoint) { update { copy(s3Endpoint = it) } }
        SyncField(stringResource(Res.string.s3_region), config.s3Region) { update { copy(s3Region = it) } }
        SyncField(stringResource(Res.string.s3_bucket), config.s3Bucket) { update { copy(s3Bucket = it) } }
        SyncField(stringResource(Res.string.s3_prefix), config.repoId) { update { copy(repoId = it) } }
        SyncField(stringResource(Res.string.s3_access_key_id), config.s3AccessKeyId) { update { copy(s3AccessKeyId = it) } }
        SyncField(stringResource(Res.string.s3_secret_access_key), config.s3SecretAccessKey, secret = true) {
            update { copy(s3SecretAccessKey = it) }
        }
    }
    SyncField(stringResource(Res.string.s3_encryption_key), config.encryptionKey, secret = true) {
        update { copy(encryptionKey = it) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(viewModel::generateEncryptionKey, enabled = !state.busy) { Text(stringResource(Res.string.s3_generate_key)) }
        Spacer(Modifier.width(8.dp))
        Button(viewModel::saveConfig, enabled = !state.busy) { Text(stringResource(Res.string.s3_save)) }
    }

    HorizontalDivider()
    WipeChunks(enabled = !state.busy, onWipe = viewModel::wipeLocalChunks)
}

@Composable
private fun WipeChunks(enabled: Boolean, onWipe: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Hint(stringResource(Res.string.s3_wipe_chunks_hint))
    OutlinedButton(
        onClick = { confirm = true },
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) { Text(stringResource(Res.string.s3_wipe_chunks)) }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(Res.string.s3_wipe_chunks)) },
            text = { Text(stringResource(Res.string.s3_wipe_chunks_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onWipe()
                }) { Text(stringResource(Res.string.s3_wipe_chunks_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

@Composable
private fun ExportConfigString(exported: String) {
    var shown by rememberSaveable { mutableStateOf(false) }
    Hint(stringResource(Res.string.sync_export_description))
    OutlinedButton(onClick = { shown = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.sync_export_string))
    }
    if (!shown) return
    @Suppress("DEPRECATION") // LocalClipboard в общем коде не умеет собрать ClipEntry из текста.
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { shown = false },
        title = { Text(stringResource(Res.string.sync_export_string)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Hint(stringResource(Res.string.sync_export_description))
                OutlinedTextField(
                    value = exported,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                if (copied) Hint(stringResource(Res.string.sync_export_copied))
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(exported))
                    copied = true
                }) { Text(stringResource(Res.string.sync_export_copy)) }
                TextButton(onClick = { shown = false }) { Text(stringResource(Res.string.close)) }
            }
        },
    )
}

/**
 * Решение по возврату со страницы сервера: подтвердить подключение или показать,
 * почему возврат отклонён.
 */
@Composable
private fun ServerConnectDialog(event: ServerConnectEvent, onResolve: (accept: Boolean) -> Unit) {
    when (event) {
        is ServerConnectEvent.Request -> AlertDialog(
            onDismissRequest = { onResolve(false) },
            title = { Text(stringResource(Res.string.sync_connect_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.sync_connect_confirm_body, event.serverUrl, event.repoId))
                    if (event.generatedKey) Hint(stringResource(Res.string.sync_connect_key_generated))
                }
            },
            confirmButton = {
                TextButton(onClick = { onResolve(true) }) { Text(stringResource(Res.string.sync_connect_action)) }
            },
            dismissButton = {
                TextButton(onClick = { onResolve(false) }) { Text(stringResource(Res.string.cancel)) }
            },
        )

        is ServerConnectEvent.Failed -> AlertDialog(
            onDismissRequest = { onResolve(false) },
            title = { Text(stringResource(Res.string.sync_connect_confirm_title)) },
            text = { Text(connectFailureText(event.failure)) },
            confirmButton = {
                TextButton(onClick = { onResolve(false) }) { Text(stringResource(Res.string.ok)) }
            },
        )
    }
}

@Composable
private fun connectFailureText(failure: ServerConnectFailure): String = when (failure) {
    ServerConnectFailure.LinkMismatch -> stringResource(Res.string.sync_connect_error_link)
    ServerConnectFailure.NoConfig -> stringResource(Res.string.sync_connect_error_no_config)
    ServerConnectFailure.OtherServer -> stringResource(Res.string.sync_connect_error_other_server)
    ServerConnectFailure.NoKey -> stringResource(Res.string.sync_connect_error_no_key)
    ServerConnectFailure.Decode -> stringResource(Res.string.sync_config_import_error)
}

@Composable
private fun SyncField(
    label: String,
    value: String,
    secret: Boolean = false,
    placeholder: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
