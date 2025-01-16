package com.example.bottomapp.ui.readerscreens.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import com.example.bottomapp.R
import com.example.bottomapp.ui.homescreen.components.IconScrollSelector
import com.example.bottomapp.ui.readerscreens.common.FlipMode
import kotlinx.coroutines.launch


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
            onStateChange(true)
        }
    }

    var hasBeenOpened by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }
            .collect { value ->
                if (value == SheetValue.Expanded || value == SheetValue.PartiallyExpanded) {
                    hasBeenOpened = true
                }
                if (value == SheetValue.Hidden && hasBeenOpened) {
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
                    getString(LocalContext.current, R.string.apply_inversion),
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
            IconScrollSelector(onVerticalScroll, scrollMode)
        }
    }
}

