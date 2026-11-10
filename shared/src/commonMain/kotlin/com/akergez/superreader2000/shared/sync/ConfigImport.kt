package com.akergez.superreader2000.shared.sync

import com.akergez.tresse.kmp.TresseConfig
import com.akergez.tresse.kmp.TresseConfigShare
import com.akergez.tresse.kmp.TresseServerLink

/** Откуда взять ключ шифрования для вставленной строки `tresse://`. */
enum class ImportKey {
    /** Строка несёт свой ключ. */
    Incoming,

    /** Сервер только что создал репозиторий (`new=1`): ключ создаём здесь. */
    Generate,

    /** Это тот же репозиторий, что уже настроен: оставляем свой ключ. */
    Current,
}

/**
 * Выбор ключа для импортируемой строки — те же правила, что у библиотечного
 * `TresseServerLink.accept`, но для строки, вставленной руками: кнопка «Скопировать»
 * на странице сервера отдаёт строку без ключа, ключ сервер никогда не видит.
 *
 * null — ключа взять неоткуда: строка указывает на чужой для устройства репозиторий,
 * и молча подставить свой ключ значило бы шифровать им чужие данные.
 */
fun chooseImportKey(incoming: TresseConfig, newRepository: Boolean, current: TresseConfig?): ImportKey? = when {
    incoming.encryptionKey.isNotBlank() -> ImportKey.Incoming
    newRepository -> ImportKey.Generate
    current != null && current.encryptionKey.isNotBlank() && current.sameRemoteAs(incoming) -> ImportKey.Current
    else -> null
}

/** Импорт отклонён по понятной пользователю причине (а не из-за битой строки). */
class ConfigImportRefused(val failure: ServerConnectFailure) : Exception(failure.name)

/**
 * Строка `tresse://` поверх конфига этой реплики. Строка — это только координаты и
 * ключи хранилища: игнор-правила, языки и расписание остаются здешними, иначе импорт
 * обнулил бы их значениями по умолчанию. Ключ выбирает [chooseImportKey]: новый
 * репозиторий (`new=1`) получает ключ, созданный здесь через [newKey], тот же
 * репозиторий сохраняет свой.
 *
 * Бросает [ConfigImportRefused], если ключ взять неоткуда, и
 * IllegalArgumentException на нераспознанный текст.
 */
suspend fun importConfigString(text: String, current: TresseConfig, newKey: suspend () -> String): TresseConfig {
    val input = text.trim()
    val incoming = TresseConfigShare.decode(input)
    val key = when (chooseImportKey(incoming, TresseServerLink.isNewRepository(input), current)) {
        ImportKey.Incoming -> incoming.encryptionKey
        ImportKey.Generate -> newKey()
        ImportKey.Current -> current.encryptionKey
        null -> throw ConfigImportRefused(ServerConnectFailure.NoKey)
    }
    return incoming.copy(
        encryptionKey = key,
        languages = current.languages,
        ignorePatterns = current.ignorePatterns,
        autoSyncEnabled = current.autoSyncEnabled,
        autoSyncIntervalMinutes = current.autoSyncIntervalMinutes,
        autoSyncDebounceSeconds = current.autoSyncDebounceSeconds,
    )
}

/** Сервер, который предлагать для входа: уже настроенный сервер Tresse или вкомпилированный. */
fun connectServerFor(current: TresseConfig, buildDefault: String): String =
    current.takeIf { it.storageType != "s3" && it.serverUrl.isNotBlank() }?.serverUrl ?: buildDefault

private fun TresseConfig.sameRemoteAs(other: TresseConfig): Boolean {
    if (storageType != other.storageType || repoId != other.repoId) return false
    return if (storageType == "s3") {
        s3Endpoint.normalizedUrl() == other.s3Endpoint.normalizedUrl() && s3Bucket == other.s3Bucket
    } else {
        serverUrl.normalizedUrl() == other.serverUrl.normalizedUrl()
    }
}

private fun String.normalizedUrl() = trim().trimEnd('/')
