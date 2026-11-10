package com.akergez.superreader2000.shared.ui.screens.syncscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.sync.ConfigImportRefused
import com.akergez.superreader2000.shared.sync.PendingConnect
import com.akergez.superreader2000.shared.sync.SERVER_CONNECT_TTL_MS
import com.akergez.superreader2000.shared.sync.ServerConnectFailure
import com.akergez.superreader2000.shared.sync.ServerConnectHost
import com.akergez.superreader2000.shared.sync.ServerConnectReturns
import com.akergez.superreader2000.shared.sync.ServerConnectStore
import com.akergez.superreader2000.shared.sync.TRESSE_SERVER_URL
import com.akergez.superreader2000.shared.sync.connectServerFor
import com.akergez.superreader2000.shared.sync.importConfigString
import com.akergez.superreader2000.shared.sync.mapServerConnectFailure
import com.akergez.tresse.kmp.TresseConfig
import com.akergez.tresse.kmp.TresseServerLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Экран синхронизации. Предпочтительный путь — вход на сервере Tresse: страница
 * сервера возвращает строку подключения, [acceptServerReturn] проверяет её через
 * `TresseServerLink.accept` и ждёт подтверждения пользователя. Строку можно вставить
 * и руками, а все поля хранилища (сервер или свой S3) — заполнить на вкладке «Подробно».
 */
class SyncViewModel(
    private val bookManager: BookManager,
    private val connectStore: ServerConnectStore,
    private val connectHost: ServerConnectHost,
) : ViewModel() {
    private val _state = MutableStateFlow(SyncScreenState(serverAddress = TRESSE_SERVER_URL))
    val state: StateFlow<SyncScreenState> = _state.asStateFlow()

    /** Слитый конфиг из возврата сервера, ждущий решения в диалоге. */
    private var connectMerged: TresseConfig? = null

    init {
        viewModelScope.launch {
            // Без выбранного каталога книг конфигу негде лежать: экран остаётся с полями по умолчанию.
            runCatching { bookManager.loadSyncConfig() }.getOrNull()?.let(::applySaved)
        }
    }

    fun updateConfig(config: TresseConfig) {
        _state.update { it.copy(config = config, status = SyncUiStatus.Idle, errorDetail = "") }
    }

    fun saveConfig() = persist(_state.value.config, SyncUiStatus.Saved)

    fun generateEncryptionKey() {
        viewModelScope.launch {
            runCatching { bookManager.generateEncryptionKey() }.fold(
                onSuccess = { key -> persist(_state.value.config.copy(encryptionKey = key), SyncUiStatus.Saved) },
                onFailure = ::fail,
            )
        }
    }

    fun synchronize() = run(SyncUiStatus.Running) { bookManager.synchronize(it) }

    fun forcePull() = run(SyncUiStatus.ForcePullRunning) { bookManager.forcePull(it) }

    /**
     * Удаляет локальные чанки Tresse и очищает repoId с ключом шифрования — и в конфиге
     * на диске, и в полях экрана. Книги и удалённое хранилище не затрагиваются.
     */
    fun wipeLocalChunks() {
        _state.update { it.copy(status = SyncUiStatus.Wiping, errorDetail = "") }
        viewModelScope.launch {
            bookManager.wipeLocalSync().fold(
                onSuccess = { config ->
                    applySaved(config)
                    _state.update { it.copy(status = SyncUiStatus.Wiped, pulled = 0, pushed = 0) }
                },
                onFailure = ::fail,
            )
        }
    }

    fun updateServerAddress(address: String) {
        _state.update { it.copy(serverAddress = address, connectBeginFailed = false) }
    }

    /** Открывает страницу сервера в браузере; возврат придёт в [ServerConnectReturns]. */
    fun beginServerConnect() {
        val server = _state.value.serverAddress.trim()
        // Адрес не https — begin бросает: для экрана это просто неудачный старт.
        val attempt = runCatching { TresseServerLink.begin(server, connectHost.returnTo) }.getOrNull()
        val started = attempt != null && connectHost.listen() && run {
            connectStore.save(PendingConnect(attempt, System.currentTimeMillis() + SERVER_CONNECT_TTL_MS))
            connectHost.open(attempt.url)
        }
        _state.update { it.copy(connectBeginFailed = !started) }
    }

    /** Забирает возврат со страницы сервера, проверяет его и спрашивает пользователя. */
    fun acceptServerReturn() {
        val returned = ServerConnectReturns.take() ?: return
        viewModelScope.launch {
            val pending = connectStore.load(System.currentTimeMillis())
            val event = if (pending == null) {
                ServerConnectEvent.Failed(ServerConnectFailure.LinkMismatch)
            } else {
                runCatching {
                    val current = currentSaved()
                    // accept просит ключ синхронно и только для нового репозитория;
                    // генерация — короткий вызов, проще сделать её заранее.
                    val freshKey = bookManager.generateEncryptionKey()
                    val merged = TresseServerLink.accept(returned, current, pending.attempt) { freshKey }
                    connectMerged = merged
                    ServerConnectEvent.Request(
                        serverUrl = merged.serverUrl,
                        repoId = merged.repoId,
                        generatedKey = merged.encryptionKey == freshKey,
                    )
                }.getOrElse { ServerConnectEvent.Failed(mapServerConnectFailure(it.message)) }
            }
            _state.update { it.copy(connectEvent = event) }
        }
    }

    /** true сохраняет подключение из возврата, false или закрытый диалог — отбрасывает. */
    fun resolveServerConnect(accept: Boolean) {
        val event = _state.value.connectEvent
        val merged = connectMerged
        connectMerged = null
        _state.update { it.copy(connectEvent = null) }
        if (accept && event is ServerConnectEvent.Request && merged != null) {
            connectStore.clear()
            persist(merged, SyncUiStatus.Saved)
        }
    }

    fun updateConnectionString(value: String) {
        _state.update { it.copy(connectionString = value, importFailure = null) }
    }

    /** Импорт строки `tresse://` — со страницы сервера или с другого устройства. */
    fun importConnectionString() {
        val text = _state.value.connectionString
        viewModelScope.launch {
            runCatching {
                importConfigString(text, currentSaved()) { bookManager.generateEncryptionKey() }
            }.fold(
                onSuccess = { imported ->
                    _state.update { it.copy(connectionString = "") }
                    persist(imported, SyncUiStatus.Imported)
                },
                onFailure = { error ->
                    val failure = (error as? ConfigImportRefused)?.failure ?: ServerConnectFailure.Decode
                    _state.update { it.copy(importFailure = failure) }
                },
            )
        }
    }

    private fun persist(config: TresseConfig, status: SyncUiStatus) {
        viewModelScope.launch {
            runCatching { bookManager.saveSyncConfig(config) }.fold(
                onSuccess = {
                    applySaved(config)
                    _state.update { it.copy(status = status, errorDetail = "") }
                },
                onFailure = ::fail,
            )
        }
    }

    private fun run(
        runningStatus: SyncUiStatus,
        action: suspend (TresseConfig) -> Result<com.akergez.superreader2000.shared.sync.SyncResult>,
    ) {
        val config = _state.value.config
        if (!config.isComplete()) {
            _state.update { it.copy(status = SyncUiStatus.ValidationError) }
            return
        }
        _state.update { it.copy(status = runningStatus, errorDetail = "") }
        viewModelScope.launch {
            action(config).fold(
                onSuccess = { result ->
                    applySaved(config)
                    _state.update { it.copy(status = SyncUiStatus.Success, pulled = result.pulled, pushed = result.pushed) }
                },
                onFailure = ::fail,
            )
        }
    }

    /**
     * Сохранённый конфиг прямо с диска: возврат с сервера может прийти раньше, чем
     * init успел его загрузить, и тогда свой ключ репозитория потерялся бы.
     */
    private suspend fun currentSaved(): TresseConfig =
        runCatching { bookManager.loadSyncConfig() }.getOrNull() ?: TresseConfig()

    private fun applySaved(config: TresseConfig) {
        _state.update { state ->
            state.copy(
                config = config,
                savedConfig = config,
                // Поле сервера следует за настроенным сервером, но не затирает то, что вписал пользователь.
                serverAddress = if (state.serverAddress.isBlank() || state.serverAddress == connectServerFor(state.savedConfig, TRESSE_SERVER_URL)) {
                    connectServerFor(config, TRESSE_SERVER_URL)
                } else {
                    state.serverAddress
                },
            )
        }
    }

    private fun fail(error: Throwable) {
        _state.update { it.copy(status = SyncUiStatus.Error, errorDetail = error.message.orEmpty()) }
    }
}
