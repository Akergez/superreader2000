package com.example.bottomapp.shared.backend

import android.content.Context
import com.example.bottomapp.shared.formats.abstractions.BookInfoExtractorFactory
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.backend.storage.BookMetaInfoDeserializer
import com.example.bottomapp.shared.backend.storage.StorageManager
import com.example.bottomapp.shared.formats.abstractions.BookMetaInfo
import com.example.bottomapp.shared.formats.abstractions.FormatType

actual class BookStorage(
    context: Context,
    settingsManager: ISettingsManager,
    adapter: BookMetaInfoDeserializer,
    infoExtractorFactory: BookInfoExtractorFactory,
    previewGeneratorMap: Map<FormatType, IPreviewGenerator>
) : StorageManager<BookMetaInfo>(
    context,
    settingsManager,
    adapter,
    adapter,
    infoExtractorFactory,
    {
        previewGeneratorMap[it.progress!!.formatType]
            ?: throw NotImplementedError("PreviewGenerator for ${it.progress!!.formatType} not found, check your DI config")
    }
)