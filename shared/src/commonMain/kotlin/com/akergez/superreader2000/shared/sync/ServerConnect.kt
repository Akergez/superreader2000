package com.akergez.superreader2000.shared.sync

import com.akergez.superreader2000.shared.backend.storage.PreferencesStore
import com.akergez.tresse.kmp.TresseServerLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Платформенная часть входа через сервер Tresse: куда сервер вернёт пользователя и
 * как открыть его страницу. Всё остальное — проверка возврата, ключ, сохранение —
 * общее и живёт в [com.akergez.superreader2000.shared.ui.screens.settingsscreen.SettingsViewModel].
 */
interface ServerConnectHost {
    /**
     * URI возврата, который страница сервера откроет с готовой строкой подключения.
     * Сервер пускает только адреса из своего списка `return_targets`.
     */
    val returnTo: String

    /** Готовится принять возврат (десктоп поднимает приёмник на localhost); false — не вышло. */
    fun listen(): Boolean

    /** Открывает [url] в браузере; false — открыть не удалось. */
    fun open(url: String): Boolean
}

/**
 * Возвраты со страницы сервера. Платформа кладёт сюда URI (Android — из интента,
 * десктоп — из приёмника на localhost), экран синхронизации забирает его через [take].
 * Возврат лежит, пока его не заберут: он мог прийти, когда открыт другой экран.
 */
object ServerConnectReturns {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun deliver(returned: String) {
        _pending.value = returned
    }

    fun take(): String? = _pending.getAndUpdate { null }
}

/** Экранные причины отказа; UI превращает каждую в строку. */
enum class ServerConnectFailure { LinkMismatch, NoConfig, OtherServer, NoKey, Decode }

/**
 * Срок жизни начатого подключения. Библиотечная попытка TTL не хранит, а за 30 минут
 * в браузере сессия на странице сервера обычно уже закрыта — старый возврат не должен
 * применяться.
 */
const val SERVER_CONNECT_TTL_MS: Long = 30 * 60 * 1000

/**
 * Раскладка исключений библиотечного `TresseServerLink.accept` по экранным причинам.
 * accept бросает require с человекочитаемыми сообщениями; при смене их текста
 * раскладка деградирует до общей [ServerConnectFailure.Decode], а не падает.
 */
fun mapServerConnectFailure(message: String?): ServerConnectFailure = when {
    message == null -> ServerConnectFailure.Decode
    "does not belong" in message -> ServerConnectFailure.LinkMismatch
    "no config" in message -> ServerConnectFailure.NoConfig
    "another server" in message -> ServerConnectFailure.OtherServer
    "encryption key" in message -> ServerConnectFailure.NoKey
    else -> ServerConnectFailure.Decode
}

/** Начатое подключение с собственным сроком жизни. */
@Serializable
data class PendingConnect(val attempt: TresseServerLink.Attempt, val expiresAtMs: Long)

/**
 * Начатое подключение переживает перезапуск: пока открыт браузер, Android может
 * выгрузить процесс, а возврат всё равно должен сматчиться с попыткой.
 */
class ServerConnectStore(private val prefs: PreferencesStore, private val json: Json) {
    fun load(nowMs: Long): PendingConnect? {
        val stored = prefs.getString(KEY, "").ifBlank { return null }
        val pending = runCatching { json.decodeFromString<PendingConnect>(stored) }.getOrNull()
        return pending?.takeIf { it.expiresAtMs > nowMs }
    }

    fun save(pending: PendingConnect) = prefs.putString(KEY, json.encodeToString(pending))

    fun clear() = prefs.removeKey(KEY)

    private companion object {
        const val KEY = "tresse_server_connect"
    }
}
