package com.akergez.superreader2000.shared.backend

import com.akergez.superreader2000.shared.formats.abstractions.BookInfoExtractorFactory
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.backend.storage.BookMetaInfoDeserializer
import com.akergez.superreader2000.shared.backend.storage.FileService
import com.akergez.superreader2000.shared.backend.storage.StorageManager
import com.akergez.superreader2000.shared.formats.abstractions.BookMetaInfo
import com.akergez.superreader2000.shared.formats.abstractions.FormatType

actual class BookStorage(
    settingsManager: ISettingsManager,
    adapter: BookMetaInfoDeserializer,
    infoExtractorFactory: BookInfoExtractorFactory,
    previewGeneratorMap: Map<FormatType, IPreviewGenerator>,
    fileService: FileService
) : StorageManager<BookMetaInfo>(
    settingsManager,
    adapter,
    adapter,
    infoExtractorFactory,
    {
        previewGeneratorMap[it.progress!!.formatType]
            ?: throw NotImplementedError("PreviewGenerator for ${it.progress!!.formatType} not found, check your DI config")
    },
    fileService
)