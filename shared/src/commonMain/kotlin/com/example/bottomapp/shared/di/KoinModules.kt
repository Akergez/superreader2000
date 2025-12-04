package com.example.bottomapp.shared.di

import com.example.bottomapp.shared.backend.bookCreators.BookCreatorFactory
import com.example.bottomapp.shared.backend.bookCreators.IBookCreateFactory
import com.example.bottomapp.shared.backend.bookExtractors.BookExtractorFactory
import com.example.bottomapp.shared.backend.bookExtractors.BookInfoExtractorFactory
import com.example.bottomapp.shared.backend.bookExtractors.DjvuBookAuthorExtractor
import com.example.bottomapp.shared.backend.bookExtractors.Fb2BookAuthorExtractor
import com.example.bottomapp.shared.backend.bookExtractors.IBookInfoExtractor
import com.example.bottomapp.shared.backend.bookExtractors.IBookInfoExtractorFactory
import com.example.bottomapp.shared.backend.bookExtractors.PdfBookAuthorExtractor
import com.example.bottomapp.shared.backend.domain.BookMetaInfo
import com.example.bottomapp.shared.backend.domain.FormatType
import com.example.bottomapp.shared.backend.previewGenerators.IPreviewGenerator
import com.example.bottomapp.shared.backend.previewGenerators.PreviewGeneratorFactory
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Common Koin module for shared business logic
 * Platform-specific modules should add to this
 */
fun commonModule(): Module = module {
    // Book extractors factory

    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.PDF)) { PdfBookAuthorExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.FB2)) { Fb2BookAuthorExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.DJVU)) { DjvuBookAuthorExtractor() }

    single {
        BookInfoExtractorFactory(
            mapOf(
                FormatType.PDF to get(qualifier = named(FormatType.PDF)),
                FormatType.FB2 to get(qualifier = named(FormatType.FB2))
            )
        )
    }

    // Book creators
    single<IBookCreateFactory>(named(FormatType.PDF)) { BookCreatorFactory().createPdfFactory() }
    single<IBookCreateFactory>(named(FormatType.DJVU)) { BookCreatorFactory().createDjvuFactory() }
    single<IBookCreateFactory>(named(FormatType.FB2)) { BookCreatorFactory().createFb2Factory() }

    // Book extractors
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.PDF)) { BookExtractorFactory().createPdfExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.DJVU)) { BookExtractorFactory().createDjvuExtractor() }
    single<IBookInfoExtractor<BookMetaInfo>>(named(FormatType.FB2)) { BookExtractorFactory().createFb2Extractor() }

    // Preview generators
    single<IPreviewGenerator>(named(FormatType.PDF)) { PreviewGeneratorFactory().createPdfPreviewGenerator() }
    single<IPreviewGenerator>(named(FormatType.DJVU)) { PreviewGeneratorFactory().createDjvuPreviewGenerator() }
    single<IPreviewGenerator>(named(FormatType.FB2)) { PreviewGeneratorFactory().createFb2PreviewGenerator() }
}

