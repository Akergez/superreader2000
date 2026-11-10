package com.akergez.superreader2000.shared.backend

import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.backend.storage.BookRepository
import com.akergez.superreader2000.shared.backend.storage.BookStorageEntry
import com.akergez.superreader2000.shared.constants.toFileExtension
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.FileType
import com.akergez.superreader2000.shared.formats.abstractions.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.akergez.superreader2000.shared.sync.SyncResult
import com.akergez.superreader2000.shared.sync.wipeChunkStore
import com.akergez.superreader2000.shared.sync.withoutRepository
import com.akergez.tresse.kmp.TresseConfig
import com.akergez.tresse.kmp.TresseConfigFile
import com.akergez.tresse.kmp.TresseSync
import com.akergez.tresse.kmp.TresseVault

class BookManager(
    private val storageManager: BookStorage,
    private val bookCache: BookRepository,
    private val messageHandler: com.akergez.superreader2000.shared.platform.MessageHandler,
    private val tresseSync: TresseSync,
    private val tresseVault: TresseVault,
) {
    suspend fun generateEncryptionKey(): String = tresseSync.generateEncryptionKey()
    /** Конфиг синхронизации этой реплики — сервер Tresse или свой S3; null, пока его нет. */
    suspend fun loadSyncConfig(): TresseConfig? = tresseSync.loadConfig()
    suspend fun saveSyncConfig(config: TresseConfig) = persistSyncConfig(config)

    suspend fun synchronize(config: TresseConfig): Result<SyncResult> = runCatching {
        metadataMutex.withLock {
            flushPendingMetadataLocked()
            persistSyncConfig(config)
            val report = tresseSync.perform(config)
            refreshAfterSync(report)
        }
    }

    suspend fun forcePull(config: TresseConfig): Result<SyncResult> = runCatching {
        metadataMutex.withLock {
            flushPendingMetadataLocked()
            persistSyncConfig(config)
            val report = tresseSync.forcePull(config)
            refreshAfterSync(report)
        }
    }

    /**
     * Сбрасывает локальную реплику: удаляет хранилище чанков и очищает repoId с ключом
     * шифрования в конфиге. Книги и удалённое хранилище не затрагиваются. Возвращает
     * очищенный конфиг, чтобы UI показал пустые поля.
     */
    suspend fun wipeLocalSync(): Result<TresseConfig> = runCatching {
        metadataMutex.withLock {
            wipeChunkStore(tresseVault)
            val cleared = (tresseSync.loadConfig() ?: TresseConfig()).withoutRepository()
            persistSyncConfig(cleared)
            cleared
        }
    }

    private suspend fun refreshAfterSync(report: com.akergez.tresse.kmp.TresseSyncResult): SyncResult {
        val books = getBooksLinksFromStorage()
        cacheBooksLinks(books)
        _bookLinks.value = books
        return SyncResult(books, report.pulled, report.pushed)
    }

    /**
     * Читалка меняет только свои учётные данные, а политика репозитория — не её.
     * `TresseSync.saveConfig` пишет оба файла разом, и общий `tresse.toml` при этом
     * перезаписывается целиком: чужие игнор-паттерны и языки, которые приехали
     * пулом или которые кто-то вписал руками, молча заменились бы на то, что лежит
     * в полях экрана. Поэтому кредлы уходят в свой нетрекаемый `remotes.toml`
     * всегда, а трекаемый `tresse.toml` создаётся ровно один раз — когда его ещё
     * нет; дальше файл принадлежит репозиторию, и читалка его не трогает.
     *
     * Создаваемый конфиг — минимальный: пустые игнор-паттерны и пустая таблица
     * `[languages]`. Пустая таблица обязательна, движок требует секцию (`missing
     * field 'languages'` без неё), а язык читалке не нужен — книги в PDF, DjVu и
     * FB2 движок по синтаксису не разбирает, всё уезжает блобами.
     */
    private fun persistSyncConfig(config: TresseConfig) {
        if (tresseVault.storeRead(TresseConfigFile.PATH) == null) {
            tresseVault.writeText(
                TresseConfigFile.PATH,
                TresseConfigFile.serialize(config.copy(ignorePatterns = emptyList(), languages = null)),
            )
        }
        tresseVault.storeWrite(
            TresseConfigFile.REMOTES_PATH,
            TresseConfigFile.serializeRemotes(config).encodeToByteArray(),
        )
    }

    private val _bookLinks = MutableStateFlow<List<BookStorageEntry>>(emptyList())
    val bookFlow = _bookLinks.asStateFlow()
    private val _pendingBooksToSave = HashSet<String>(emptyList())
    // Метаданные книги (включая progress) являются одним синхронизируемым файлом.
    // Локальная запись и sync не должны работать с ним одновременно.
    private val metadataMutex = Mutex()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            bookCache.bookFlow.collect {
                _bookLinks.value = it
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            refreshBooksLinks()
        }
    }

    suspend fun getBookFile(bundleName: String): Result<FileWrapper> {
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))
        return storageManager.getBookFile(bundleName, meta.bookMetaInfo)
    }

    suspend fun addNewBook(
        fileUri: String,
        fileName: String,
        progress: Progress,
        fileType: FileType
    ): Result<Unit> {
        storageManager.createBundle(
            fileName,
            fileUri,
            BookMetaInfo(fileName, progress),
            fileType.toFileExtension()
        ).onSuccess {
            CoroutineScope(Dispatchers.IO).launch {
                bookCache.addBook(
                    BookStorageEntry(
                        fileName,
                        it.previewUri,
                        fileName,
                        it.meta
                    )
                )
            }
            return Result.success(Unit)
        }.onFailure {
            messageHandler.showMessage(it.message ?: "Failed to add book")
            return Result.failure(it)
        }
        return Result.success(Unit)
    }

    suspend fun refreshBooksLinks() {
        val list = getBooksLinksFromStorage()
        cacheBooksLinks(list)
    }

    suspend fun updateProgress(bundleName: String, bookMetaInfo: BookMetaInfo) {
        metadataMutex.withLock {
            val book = bookCache.findBook(bundleName)
            val currentMeta = book?.bookMetaInfo ?: bookMetaInfo
            val updatedMeta = currentMeta.copy(
                progress = bookMetaInfo.progress,
            )
            bookCache.updateBook(
                BookStorageEntry(
                    updatedMeta.title,
                    book?.previewUri,
                    bundleName,
                    updatedMeta
                )
            )
            _pendingBooksToSave.add(bundleName)
            saveBookLinkLocked(bundleName).getOrThrow()
        }
    }

    suspend fun deleteBook(bundleName: String): Result<Unit> {
        storageManager.deleteBundle(bundleName)
            .onSuccess {
                refreshBooksLinks()
                return Result.success(Unit)
            }.onFailure {
                return Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    suspend fun renameBook(bundleName: String, newNameForBundle: String): Result<Unit> {
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))

        storageManager.renameBundle(bundleName, newNameForBundle, meta.bookMetaInfo)
            .onSuccess {
                storageManager.updateBundleMeta(
                    newNameForBundle,
                    BookMetaInfo(newNameForBundle, meta.bookMetaInfo.progress)
                ).onSuccess {
                    refreshBooksLinks()
                    return Result.success(Unit)
                }.onFailure {
                    storageManager.renameBundle(newNameForBundle, bundleName, meta.bookMetaInfo)
                    return Result.failure<Unit>(it)
                }
            }
            .onFailure {
                return Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    suspend fun updateAuthor(bundleName: String, author: String?): Result<Unit> {
        println("BookManager - updateAuthor: $author")
        val meta = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))

        storageManager.updateBundleMeta(bundleName, meta.bookMetaInfo.copy(author = author))
            .onSuccess {
                refreshBooksLinks()
                return Result.success(Unit)
            }
            .onFailure {
                return Result.failure<Unit>(it)
            }
        return Result.success(Unit)
    }

    suspend fun updateBookPreview(
        bundleName: String,
        newPreviewUri: String
    ): Result<String> {
        val newPreview = storageManager.updateBundlePreview(newPreviewUri, bundleName)
        newPreview.onSuccess {
            refreshBooksLinks()
            return Result.success(it)
        }.onFailure {
            return Result.failure<String>(it.cause ?: it)
        }
        return Result.failure(Exception("Failed to update preview"))
    }

    suspend fun saveBookLinks() {
        println("BookManager - saveBookLinks")
        metadataMutex.withLock {
            flushPendingMetadataLocked()
        }
    }

    suspend fun saveBookLink(bundleName: String): Result<Unit> = metadataMutex.withLock {
        saveBookLinkLocked(bundleName)
    }

    private suspend fun flushPendingMetadataLocked() {
        _pendingBooksToSave.toList().forEach { bundleName ->
            saveBookLinkLocked(bundleName).getOrThrow()
        }
    }

    /** Вызывать только под metadataMutex. */
    private suspend fun saveBookLinkLocked(bundleName: String): Result<Unit> {
        val book = bookCache.findBook(bundleName)
            ?: return Result.failure(Exception("Book not found"))
        val result = storageManager.updateBundleMeta(bundleName, book.bookMetaInfo)
        if (result.isSuccess) {
            _pendingBooksToSave.remove(bundleName)
        }
        return result
    }

    private suspend fun getBooksLinksFromStorage(): List<BookStorageEntry> {
        return storageManager.listAllBundles().filterNotNull().map {
            BookStorageEntry(
                it.meta.title,
                it.previewUri,
                it.bundleName,
                it.meta
            )
        }
    }

    private suspend fun cacheBooksLinks(bookLinks: List<BookStorageEntry>) {
        bookCache.saveBooks(bookLinks)
    }
}
