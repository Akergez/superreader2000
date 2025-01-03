package com.example.bottomapp.ui.readerscreens.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import com.example.bottomapp.R


@Composable
fun SettingsModal(
    sheetState: ModalBottomSheetState,
    onApplyInversionToggle: (Boolean) -> Unit,
    applyInversion: Boolean,
) {
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
//                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(
                    text = getString(LocalContext.current, R.string.edit_book),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Инвертировать тему", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = applyInversion,
                        onCheckedChange = { onApplyInversionToggle(!applyInversion)  }
                    )
                }


            }
        }
    ) {  }
}

