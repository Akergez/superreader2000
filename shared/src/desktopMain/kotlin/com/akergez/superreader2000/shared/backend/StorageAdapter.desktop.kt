package com.akergez.superreader2000.shared.backend

import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.backend.storage.FileService
import com.akergez.superreader2000.shared.backend.storage.IDeserializer
import com.akergez.superreader2000.shared.backend.storage.INameProvider
import com.akergez.superreader2000.shared.backend.storage.StorageManager
import com.akergez.superreader2000.shared.formats.abstractions.BookInfoExtractorFactory
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.FormatType

actual class BookStorage(
    settingsManager: ISettingsManager,
    deserializer: IDeserializer<BookMetaInfo>,
    nameProvider: INameProvider<BookMetaInfo>,
    infoExtractor: BookInfoExtractorFactory,
    previewGeneratorMap: Map<FormatType, IPreviewGenerator>,
    fileService: FileService
) :
    StorageManager<BookMetaInfo>(
        settingsManager,
        deserializer,
        nameProvider,
        infoExtractor,
        {
            previewGeneratorMap[it.progress!!.formatType]
                ?: throw NotImplementedError("PreviewGenerator for ${it.progress!!.formatType} not found, check your DI config")
        },
        fileService
    )