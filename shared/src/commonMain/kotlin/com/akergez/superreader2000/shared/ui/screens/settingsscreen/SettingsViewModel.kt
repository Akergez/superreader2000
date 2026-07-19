package com.akergez.superreader2000.shared.ui.screens.settingsscreen

import androidx.lifecycle.ViewModel
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.backend.settings.ReaderFont
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.akergez.tresse.kmp.TresseConfig
import com.akergez.tresse.kmp.TresseConfigShare
import kotlinx.serialization.json.Json

class SettingsViewModel(
    private val settingsManager: ISettingsManager,
    private val bookManager: BookManager,
    private val json: Json
) : ViewModel() {
    private val _settingsState = MutableStateFlow(SettingsScreenState())
    val settingsState: StateFlow<SettingsScreenState> = _settingsState.asStateFlow()
    private val viewModelJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    init {
        val textDocThemeMode = settingsManager.getTextDocumentsTheme(DocThemeMode.AsIs)
        val fixedDocThemeMode = settingsManager.getFixedDocumentsTheme(DocThemeMode.AsIs)
        val pdfFlipMode = settingsManager.getFlipMode()
        val storageAddress = extractBeautifulStoragePath(settingsManager.getStoragePath())
        val appThemeMode = settingsManager.getAppTheme(AppThemeMode.System)
        _settingsState.value =
            SettingsScreenState(
                storageAddress = storageAddress,
                textDocThemeMode = textDocThemeMode,
                fixedDocThemeMode = fixedDocThemeMode,
                flipMode = pdfFlipMode,
                appThemeMode = appThemeMode,
                readerFont = settingsManager.getReaderFont(),
                readerFontSize = settingsManager.getReaderFontSize(),
                compatibilityTextRenderer = settingsManager.getCompatibilityTextRenderer(),
                s3Config = TresseConfig(storageType = "s3", repoId = "")
            )
        if (settingsManager.getStoragePath() != null) {
            uiScope.launch {
                runCatching { bookManager.loadS3Config() }.getOrNull()?.let { config ->
                    _settingsState.value = _settingsState.value.copy(s3Config = config)
                }
            }
        }
    }

    fun updateTextDocThemeMode(docThemeMode: DocThemeMode) {
        settingsManager.setTextDocumentsTheme(docThemeMode)
        _settingsState.value = _settingsState.value.copy(textDocThemeMode = docThemeMode)
    }

    fun updateFixedDocThemeMode(docThemeMode: DocThemeMode) {
        settingsManager.setFixedDocumentsTheme(docThemeMode)
        _settingsState.value = _settingsState.value.copy(fixedDocThemeMode = docThemeMode)
    }

    fun updateDocThemeMode(docThemeMode: DocThemeMode) = updateFixedDocThemeMode(docThemeMode)

    fun updateFlipMode(pdfFlipMode: FlipMode) {
        settingsManager.setFlipMode(pdfFlipMode)
        _settingsState.value = _settingsState.value.copy(flipMode = pdfFlipMode)
    }

    fun updateReaderFont(font: ReaderFont) {
        settingsManager.setReaderFont(font)
        _settingsState.value = _settingsState.value.copy(readerFont = font)
    }

    fun updateReaderFontSize(size: Float) {
        settingsManager.setReaderFontSize(size)
        _settingsState.value = _settingsState.value.copy(readerFontSize = size)
    }

    fun updateCompatibilityTextRenderer(enabled: Boolean) {
        settingsManager.setCompatibilityTextRenderer(enabled)
        _settingsState.value = _settingsState.value.copy(compatibilityTextRenderer = enabled)
    }

    fun updateStorageAddress(storageAddress: String) {
        settingsManager.setStoragePath(storageAddress)
        uiScope.launch {
            bookManager.refreshBooksLinks()
        }
        _settingsState.value = _settingsState.value.copy(storageAddress = extractBeautifulStoragePath(settingsManager.getStoragePath()))
    }

    fun updateAppThemeMode(appThemeMode: AppThemeMode) {
        settingsManager.setAppTheme(appThemeMode)
        _settingsState.value = _settingsState.value.copy(appThemeMode = appThemeMode)
    }

    fun updateS3Config(config: TresseConfig) {
        _settingsState.value = _settingsState.value.copy(
            s3Config = config.copy(storageType = "s3"),
            syncStatus = SyncUiStatus.Idle,
            syncErrorDetail = "",
        )
    }

    fun saveS3Config() {
        uiScope.launch {
            runCatching { bookManager.saveS3Config(_settingsState.value.s3Config) }.fold(
                onSuccess = { _settingsState.value = _settingsState.value.copy(syncStatus = SyncUiStatus.Saved) },
                onFailure = { error -> _settingsState.value = _settingsState.value.copy(syncStatus = SyncUiStatus.Error, syncErrorDetail = error.message.orEmpty()) },
            )
        }
    }

    fun generateEncryptionKey() {
        uiScope.launch {
            runCatching { bookManager.generateEncryptionKey() }.fold(
                onSuccess = { key ->
                    val config = _settingsState.value.s3Config.copy(encryptionKey = key)
                    bookManager.saveS3Config(config)
                    _settingsState.value = _settingsState.value.copy(s3Config = config, syncStatus = SyncUiStatus.Saved)
                },
                onFailure = { error ->
                    _settingsState.value = _settingsState.value.copy(syncStatus = SyncUiStatus.Error, syncErrorDetail = error.message.orEmpty())
                }
            )
        }
    }

    fun synchronize() {
        val config = _settingsState.value.s3Config
        if (!isValidS3Config(config)) {
            _settingsState.value = _settingsState.value.copy(syncStatus = SyncUiStatus.ValidationError)
            return
        }
        _settingsState.value = _settingsState.value.copy(syncStatus = SyncUiStatus.Running, syncErrorDetail = "")
        uiScope.launch {
            bookManager.synchronize(config).fold(
                onSuccess = { result ->
                    _settingsState.value = _settingsState.value.copy(
                        syncStatus = SyncUiStatus.Success, pulled = result.pulled, pushed = result.pushed
                    )
                },
                onFailure = { error ->
                    _settingsState.value = _settingsState.value.copy(
                        syncStatus = SyncUiStatus.Error, syncErrorDetail = error.message.orEmpty()
                    )
                }
            )
        }
    }

    fun forcePull() {
        val config = _settingsState.value.s3Config
        if (!isValidS3Config(config)) {
            _settingsState.value = _settingsState.value.copy(syncStatus = SyncUiStatus.ValidationError)
            return
        }
        _settingsState.value = _settingsState.value.copy(syncStatus = SyncUiStatus.ForcePullRunning, syncErrorDetail = "")
        uiScope.launch {
            bookManager.forcePull(config).fold(
                onSuccess = { result ->
                    _settingsState.value = _settingsState.value.copy(
                        syncStatus = SyncUiStatus.Success, pulled = result.pulled, pushed = result.pushed
                    )
                },
                onFailure = { error ->
                    _settingsState.value = _settingsState.value.copy(
                        syncStatus = SyncUiStatus.Error, syncErrorDetail = error.message.orEmpty()
                    )
                }
            )
        }
    }

    fun updateConfigShareString(value: String) {
        _settingsState.value = _settingsState.value.copy(configShareString = value)
    }

    fun exportS3Config() {
        _settingsState.value = _settingsState.value.copy(
            configShareString = TresseConfigShare.encode(_settingsState.value.s3Config.copy(storageType = "s3"), json),
            syncStatus = SyncUiStatus.ExportWarning
        )
    }

    fun importS3Config() {
        runCatching {
            val configString = _settingsState.value.configShareString.trim()
            require(configString.startsWith(TresseConfigShare.PREFIX)) {
                "config string must start with ${TresseConfigShare.PREFIX}"
            }
            TresseConfigShare.decode(configString, json).also { config ->
                require(config.storageType == "s3") {
                    "config uses backend '${config.storageType}'; this app only supports s3"
                }
            }
        }.fold(
            onSuccess = { config ->
                uiScope.launch {
                    runCatching { bookManager.saveS3Config(config) }.fold(
                        onSuccess = { _settingsState.value = _settingsState.value.copy(s3Config = config, syncStatus = SyncUiStatus.Imported) },
                        onFailure = { error -> _settingsState.value = _settingsState.value.copy(syncStatus = SyncUiStatus.ImportError, syncErrorDetail = error.message.orEmpty()) },
                    )
                }
            },
            onFailure = { error ->
                _settingsState.value = _settingsState.value.copy(
                    syncStatus = SyncUiStatus.ImportError, syncErrorDetail = error.message.orEmpty()
                )
            }
        )
    }

    private fun extractBeautifulStoragePath(path: String?) : String {
        return path ?: ""
    }

    private fun isValidS3Config(config: TresseConfig): Boolean =
        config.s3Endpoint.isNotBlank() &&
            config.s3Bucket.isNotBlank() &&
            config.s3AccessKeyId.isNotBlank() &&
            config.s3SecretAccessKey.isNotBlank() &&
            config.encryptionKey.isNotBlank()
}
