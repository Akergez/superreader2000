package com.akergez.superreader2000.shared.ui.screens.syncscreen

import com.akergez.superreader2000.shared.sync.ServerConnectFailure
import com.akergez.tresse.kmp.TresseConfig
import com.akergez.tresse.kmp.TresseConfigShare

enum class SyncUiStatus { Idle, Running, ForcePullRunning, Success, Error, Saved, Imported, ValidationError, Wiping, Wiped }

/**
 * Возврат со страницы сервера, ждущий решения пользователя: подтвердить подключение
 * ([Request]) или узнать, почему возврат отклонён ([Failed]).
 */
sealed interface ServerConnectEvent {
    data class Request(val serverUrl: String, val repoId: String, val generatedKey: Boolean) : ServerConnectEvent
    data class Failed(val failure: ServerConnectFailure) : ServerConnectEvent
}

data class SyncScreenState(
    /** Конфиг в полях вкладки «Подробно»; по умолчанию — сервер Tresse. */
    val config: TresseConfig = TresseConfig(),
    /** Последний сохранённый конфиг: от него экспорт и выбор ключа при импорте. */
    val savedConfig: TresseConfig = TresseConfig(),
    val status: SyncUiStatus = SyncUiStatus.Idle,
    val pulled: Int = 0,
    val pushed: Int = 0,
    val errorDetail: String = "",
    /** Адрес сервера для входа: уже настроенный или вкомпилированный при сборке. */
    val serverAddress: String = "",
    val connectBeginFailed: Boolean = false,
    val connectEvent: ServerConnectEvent? = null,
    val connectionString: String = "",
    val importFailure: ServerConnectFailure? = null,
) {
    val busy: Boolean get() = status in setOf(SyncUiStatus.Running, SyncUiStatus.ForcePullRunning, SyncUiStatus.Wiping)

    /** Строка для следующего устройства — только когда синк настроен полностью. */
    val exportString: String? get() = savedConfig.takeIf { it.isComplete() }?.let(TresseConfigShare::encode)
}
