package com.akergez.superreader2000.shared.sync

import com.akergez.tresse.kmp.TresseConfig
import com.akergez.tresse.kmp.TresseVault

/** Классы объектов локального CAS — всё под ними восстанавливается новой синхронизацией. */
private val STORE_DIRS = listOf(".tresse/meta", ".tresse/content")
private const val FILE_INDEX = ".tresse/file-index.json"

/**
 * Удаляет локальное хранилище чанков реплики (оба класса объектов и файловый индекс),
 * не трогая ни `.tresse/config.toml`, ни сами книги. Возвращает число удалённых объектов.
 */
fun wipeChunkStore(vault: TresseVault): Int {
    var removed = 0
    for (dir in STORE_DIRS) {
        val names = runCatching { vault.storeList(dir) }.getOrDefault(emptyList())
        for (name in names) {
            runCatching { vault.storeDelete("$dir/$name") }.onSuccess { removed += 1 }
        }
    }
    runCatching { vault.storeDelete(FILE_INDEX) }
    return removed
}

/** Забывает, какому репозиторию принадлежала реплика; координаты хранилища остаются. */
fun TresseConfig.withoutRepository(): TresseConfig = copy(repoId = "", encryptionKey = "")
