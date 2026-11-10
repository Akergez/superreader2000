package com.akergez.superreader2000.shared

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.BookStorage
import com.akergez.superreader2000.shared.backend.storage.AppDatabase
import com.akergez.superreader2000.shared.backend.storage.BookMetaInfoDeserializer
import com.akergez.superreader2000.shared.backend.storage.BookRepository
import com.akergez.superreader2000.shared.backend.storage.FileService
import com.akergez.superreader2000.shared.formats.abstractions.BookInfoExtractorFactory
import com.akergez.superreader2000.shared.formats.abstractions.Progress
import com.akergez.superreader2000.shared.formats.djvu.DjvuProgress
import com.akergez.superreader2000.shared.formats.fb2.Fb2Progress
import com.akergez.superreader2000.shared.formats.pdf.PdfProgress
import com.akergez.superreader2000.shared.platform.MessageHandler
import kotlinx.serialization.json.Json
import com.akergez.superreader2000.shared.di.SettingsBackedFileTresseVault
import com.akergez.tresse.kmp.TresseSync
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val testJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    serializersModule = SerializersModule {
        polymorphic(Progress::class) {
            subclass(PdfProgress::class)
            subclass(Fb2Progress::class)
            subclass(DjvuProgress::class)
        }
    }
}

fun buildTestDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()

fun buildTestBookManager(
    db: AppDatabase,
    settingsManager: FakeSettingsManager = FakeSettingsManager()
): BookManager {
    val deserializer = BookMetaInfoDeserializer(testJson)
    val bookStorage = BookStorage(
        settingsManager,
        deserializer,
        deserializer,
        BookInfoExtractorFactory(emptyMap()),
        emptyMap(),
        FileService()
    )
    val vault = SettingsBackedFileTresseVault(settingsManager)
    val tresse = TresseSync(vault)
    return BookManager(bookStorage, BookRepository(db.userDao(), testJson), MessageHandler(), tresse, vault)
}
