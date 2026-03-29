package com.akergez.superreader2000.shared.ui.screens.readerscreens.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.apply_inversion


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModal(
    onApplyInversionToggle: (Boolean) -> Unit,
    applyInversion: Boolean,
    onVerticalScroll: (FlipMode) -> Unit,
    scrollMode: FlipMode,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                   stringResource( Res.string.apply_inversion),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = applyInversion,
                    onCheckedChange = { onApplyInversionToggle(!applyInversion) },
                    colors = SwitchDefaults.colors()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
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

