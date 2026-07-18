package com.akergez.superreader2000.shared.ui.screens.readerscreens.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.akergez.superreader2000.shared.ui.screens.settingsscreen.components.IconScrollSelector
import com.akergez.superreader2000.shared.ui.screens.settingsscreen.ThemeSelector
import com.akergez.superreader2000.shared.backend.settings.MAX_READER_FONT_SIZE
import com.akergez.superreader2000.shared.backend.settings.MIN_READER_FONT_SIZE
import com.akergez.superreader2000.shared.backend.settings.ReaderFont
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.documents_theme
import superreader2000.shared.generated.resources.documents_theme_black_on_white
import superreader2000.shared.generated.resources.documents_theme_material_you
import superreader2000.shared.generated.resources.documents_theme_soft_dark
import superreader2000.shared.generated.resources.documents_theme_soft_light
import superreader2000.shared.generated.resources.documents_theme_white_on_black
import superreader2000.shared.generated.resources.reader_font
import superreader2000.shared.generated.resources.reader_font_sans_serif
import superreader2000.shared.generated.resources.reader_font_serif
import superreader2000.shared.generated.resources.reader_font_size


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModal(
    documentTheme: DocThemeMode,
    availableDocumentThemes: List<DocThemeMode>,
    onDocumentThemeChange: (DocThemeMode) -> Unit,
    onVerticalScroll: (FlipMode) -> Unit,
    scrollMode: FlipMode,
    readerFont: ReaderFont,
    readerFontSize: Float,
    readerTypographyEnabled: Boolean,
    onReaderFontChange: (ReaderFont) -> Unit,
    onReaderFontSizeChange: (Float) -> Unit,
    onStateChange: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            sheetState.show()
            // onStateChange(true) убран — showSettingsModal уже true, иначе SettingsModal
            // не попал бы в композицию. Повторный вызов создавал гонку с onStateChange(false):
            // hide() завершался, snapshotFlow вызывал false, а этот true выполнялся позже.
        }
    }

    var hasBeenOpened by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }
            .collect { value ->
                println(">>> SettingsModal sheetState=$value hasBeenOpened=$hasBeenOpened")
                if (value == SheetValue.Expanded || value == SheetValue.PartiallyExpanded) {
                    hasBeenOpened = true
                }
                if (value == SheetValue.Hidden && hasBeenOpened) {
                    println(">>> SettingsModal: calling onStateChange(false)")
                    onStateChange(false)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { coroutineScope.launch { sheetState.hide() } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
    {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            val themeLabels = mapOf(
                DocThemeMode.AsIs to stringResource(Res.string.documents_theme_black_on_white),
                DocThemeMode.Invert to stringResource(Res.string.documents_theme_white_on_black),
                DocThemeMode.SoftLight to stringResource(Res.string.documents_theme_soft_light),
                DocThemeMode.SoftDark to stringResource(Res.string.documents_theme_soft_dark),
                DocThemeMode.MaterialYou to stringResource(Res.string.documents_theme_material_you),
            )
            val effectiveTheme = documentTheme.takeIf { it in availableDocumentThemes }
                ?: availableDocumentThemes.first()
            ThemeSelector(
                stringResource(Res.string.documents_theme),
                availableDocumentThemes.associateWith { themeLabels.getValue(it) },
                effectiveTheme,
                onDocumentThemeChange,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (readerTypographyEnabled) {
                ThemeSelector(
                    stringResource(Res.string.reader_font),
                    mapOf(
                    ReaderFont.Literata to stringResource(Res.string.reader_font_serif),
                    ReaderFont.Roboto to stringResource(Res.string.reader_font_sans_serif),
                    ),
                    readerFont,
                    onReaderFontChange,
                )
                Text(stringResource(Res.string.reader_font_size, readerFontSize.toInt()))
                Slider(
                    value = readerFontSize,
                    onValueChange = onReaderFontSizeChange,
                    valueRange = MIN_READER_FONT_SIZE..MAX_READER_FONT_SIZE,
                    steps = (MAX_READER_FONT_SIZE - MIN_READER_FONT_SIZE).toInt() - 1,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            // Скрываем шторку перед вызовом onVerticalScroll, чтобы не было ghost-click:
            // detectTapGestures поглощает DOWN, но если модал исчезает без анимации,
            // UP-событие падает на кнопку Settings в карусели и повторно открывает шторку.
            IconScrollSelector(
                onSelect = { mode ->
                    coroutineScope.launch {
                        println(">>> SettingsModal: hiding sheet before onVerticalScroll($mode)")
                        sheetState.hide()
                        onVerticalScroll(mode)
                    }
                },
                selected = scrollMode,
            )
        }
    }
}
