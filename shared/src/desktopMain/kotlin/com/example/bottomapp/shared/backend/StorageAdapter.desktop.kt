package com.example.bottomapp.shared.backend

import com.example.bottomapp.shared.backend.bookExtractors.BookInfoExtractorFactory
import com.example.bottomapp.shared.backend.bookExtractors.IBookInfoExtractorFactory
import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.settings.DefaultSettingsManager
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.backend.storage.Book
import com.example.bottomapp.shared.backend.storage.IDeserializer
import com.example.bottomapp.shared.backend.storage.INameProvider
import com.example.bottomapp.shared.backend.storage.StorageManager

actual class BookStorage(
    settingsManager: ISettingsManager,
    deserializer: IDeserializer<BookMetaInfo>,
    nameProvider: INameProvider<BookMetaInfo>,
    infoExtractor: BookInfoExtractorFactory,
    previewGeneratorMap: Map<FormatType, IPreviewGenerator>
) :
    StorageManager<BookMetaInfo>(
        settingsManager,
        deserializer,
        nameProvider,
        infoExtractor,
        {
            previewGeneratorMap[it.progress!!.formatType]
                ?: throw NotImplementedError("PreviewGenerator for ${it.progress!!.formatType} not found, check your DI config")
        }
    )