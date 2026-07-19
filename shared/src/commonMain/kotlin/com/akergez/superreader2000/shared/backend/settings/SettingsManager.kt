package com.akergez.superreader2000.shared.backend.settings

import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Application theme modes
 */
@Serializable
enum class AppThemeMode {
    Light,
    Dark,
    System
}

/**
 * Document display theme modes
 */
enum class DocThemeMode {
    AsIs,
    Invert,
    SoftLight,
    SoftDark,
    MaterialYou,
}

val ALL_DOC_THEME_MODES = DocThemeMode.entries.toList()
val BINARY_DOC_THEME_MODES = listOf(DocThemeMode.AsIs, DocThemeMode.Invert)

enum class ReaderFont { Literata, Roboto }

const val DEFAULT_READER_FONT_SIZE = 18f
const val MIN_READER_FONT_SIZE = 12f
const val MAX_READER_FONT_SIZE = 32f

interface ISettingsManager {
    val appThemeModeFlow: StateFlow<AppThemeMode>
     fun getAppTheme(defaultTheme: AppThemeMode = AppThemeMode.System): AppThemeMode
     fun setAppTheme(theme: AppThemeMode)
     fun getDocumentsTheme(defaultTheme: DocThemeMode = DocThemeMode.AsIs): DocThemeMode
     fun setDocumentsTheme(theme: DocThemeMode)
     fun getTextDocumentsTheme(defaultTheme: DocThemeMode = DocThemeMode.AsIs): DocThemeMode
     fun setTextDocumentsTheme(theme: DocThemeMode)
     fun getFixedDocumentsTheme(defaultTheme: DocThemeMode = DocThemeMode.AsIs): DocThemeMode
     fun setFixedDocumentsTheme(theme: DocThemeMode)
     fun getFlipMode(): FlipMode
     fun setFlipMode(mode: FlipMode)
     fun getReaderFont(): ReaderFont
     fun setReaderFont(font: ReaderFont)
     fun getReaderFontSize(): Float
     fun setReaderFontSize(size: Float)
     fun getCompatibilityTextRenderer(): Boolean
     fun setCompatibilityTextRenderer(enabled: Boolean)
     fun getStoragePath(): String?
     fun setStoragePath(path: String)
     fun clearStoragePath()
}

class DefaultSettingsManager(
    private val store: com.akergez.superreader2000.shared.backend.storage.PreferencesStore
) : ISettingsManager {
    private val _appThemeModeFlow = kotlinx.coroutines.flow.MutableStateFlow(AppThemeMode.System)
    override val appThemeModeFlow: StateFlow<AppThemeMode> = _appThemeModeFlow

    override  fun getAppTheme(defaultTheme: AppThemeMode): AppThemeMode {
        val themeName = store.getString("app_theme", defaultTheme.name)
        return try {
            AppThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            defaultTheme
        }
    }

    override  fun setAppTheme(theme: AppThemeMode) {
        store.putString("app_theme", theme.name)
        _appThemeModeFlow.value = theme
    }

    override  fun getDocumentsTheme(defaultTheme: DocThemeMode): DocThemeMode {
        val themeName = store.getString("doc_theme", defaultTheme.name)
        return try {
            DocThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            defaultTheme
        }
    }

    override  fun setDocumentsTheme(theme: DocThemeMode) {
        store.putString("doc_theme", theme.name)
    }

    private fun getStoredTheme(key: String, defaultTheme: DocThemeMode): DocThemeMode {
        val legacyTheme = store.getString("doc_theme", defaultTheme.name)
        return runCatching {
            DocThemeMode.valueOf(store.getString(key, legacyTheme))
        }.getOrDefault(defaultTheme)
    }

    override fun getTextDocumentsTheme(defaultTheme: DocThemeMode) =
        getStoredTheme("text_doc_theme", defaultTheme)

    override fun setTextDocumentsTheme(theme: DocThemeMode) =
        store.putString("text_doc_theme", theme.name)

    override fun getFixedDocumentsTheme(defaultTheme: DocThemeMode): DocThemeMode {
        val theme = getStoredTheme("fixed_doc_theme", defaultTheme)
        return if (theme in BINARY_DOC_THEME_MODES) theme else defaultTheme
    }

    override fun setFixedDocumentsTheme(theme: DocThemeMode) =
        store.putString("fixed_doc_theme", theme.takeIf { it in BINARY_DOC_THEME_MODES }?.name ?: DocThemeMode.AsIs.name)

    override  fun getFlipMode(): FlipMode {
        val modeName = store.getString("pdf_flip_mode", FlipMode.PagingCurl.name)
        return try {
            FlipMode.valueOf(modeName)
        } catch (e: Exception) {
            FlipMode.PagingCurl
        }
    }

    override  fun setFlipMode(mode: FlipMode) {
        store.putString("pdf_flip_mode", mode.name)
    }

    override fun getReaderFont(): ReaderFont {
        val storedValue = store.getString("reader_font", ReaderFont.Literata.name)
        return when (storedValue) {
            "Serif" -> ReaderFont.Literata
            "SansSerif" -> ReaderFont.Roboto
            else -> runCatching { ReaderFont.valueOf(storedValue) }
                .getOrDefault(ReaderFont.Literata)
        }
    }

    override fun setReaderFont(font: ReaderFont) = store.putString("reader_font", font.name)

    override fun getReaderFontSize(): Float = store
        .getString("reader_font_size", DEFAULT_READER_FONT_SIZE.toString())
        .toFloatOrNull()
        ?.coerceIn(MIN_READER_FONT_SIZE, MAX_READER_FONT_SIZE)
        ?: DEFAULT_READER_FONT_SIZE

    override fun setReaderFontSize(size: Float) {
        store.putString("reader_font_size", size.coerceIn(MIN_READER_FONT_SIZE, MAX_READER_FONT_SIZE).toString())
    }

    override fun getCompatibilityTextRenderer(): Boolean =
        store.getString("compatibility_text_renderer", "false").toBooleanStrictOrNull() ?: false

    override fun setCompatibilityTextRenderer(enabled: Boolean) =
        store.putString("compatibility_text_renderer", enabled.toString())

    override  fun getStoragePath(): String? {
        val path = store.getString("storage_path", "")
        return path.ifEmpty { null }
    }

    override  fun setStoragePath(path: String) {
        store.putString("storage_path", path)
    }

    override  fun clearStoragePath() {
        store.removeKey("storage_path")
    }

}
