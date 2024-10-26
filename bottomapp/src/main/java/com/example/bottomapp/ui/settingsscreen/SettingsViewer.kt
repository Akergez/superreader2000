@file:OptIn(ExperimentalFoundationApi::class)

package com.example.bottomapp.ui.settingsscreen

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bottomapp.MainActivity
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.FlipMode
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    updateAppTheme: (AppThemeMode) -> Unit,
    appTheme: AppThemeMode,
    settingsManager: SettingsManager = koinInject()
) {
    var docTheme by remember { mutableStateOf(settingsManager.getDocumentsTheme(DocThemeMode.AsIs)) } // State to control inversion
    var pdfFlipMode by remember { mutableStateOf(settingsManager.getPdfFlipMode()) }

    var storageAddress by remember { mutableStateOf(settingsManager.getStorageUri().toString()) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("App Theme", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            AppThemeMode.entries.forEach { theme ->
                RadioButton(selected = appTheme == theme, onClick = { updateAppTheme(theme) })
                Text(text = when (theme) {
                    AppThemeMode.Light -> "Светлая"
                    AppThemeMode.Dark -> "Темная"
                    AppThemeMode.System -> "Системная"
                },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable { }
                        .padding(start = 8.dp))
            }
        }

        Text("Documents Theme", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            DocThemeMode.entries.forEach { theme ->
                RadioButton(
                    selected = docTheme == theme,
                    onClick = { docTheme = theme; settingsManager.setDocumentsTheme(docTheme) })
                Text(text = theme.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable { }
                        .padding(start = 8.dp))
            }
        }


        Text("Режим листания", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            arrayOf(FlipMode.Curl, FlipMode.Scroll).forEach { mode ->
                RadioButton(selected = pdfFlipMode == mode, onClick = {
                    pdfFlipMode = mode
                    settingsManager.setPdfFlipMode(mode)
                })
                Text(
                    text = when (mode) {
                        FlipMode.Curl -> "Классический"
                        FlipMode.Scroll -> "Вертикальный"
                        else -> "Неизвестный"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable {
                            pdfFlipMode = mode
                            settingsManager.setPdfFlipMode(mode)
                        }
                        .padding(start = 8.dp))
            }
        }

        Row(Modifier.padding(32.dp)) {

        }

        Text("Storage address", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            Text(
                text = storageAddress,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            (context as MainActivity).openDirectoryPicker()
                            // не обновляет сразу, т.к. как я понял openDirectoryPicker запускаает выбор в другом потоке
                            storageAddress = settingsManager.getStorageUri().toString()
                        },
                        onLongClick = {
                            val longPressText =
                                "Не обновляет сразу, т.к. как я понял openDirectoryPicker запускаает выбор в другом потоке"
                            Toast.makeText(context, longPressText, Toast.LENGTH_LONG).show()
                            storageAddress = settingsManager.getStorageUri().toString()
                        },
                    )
                    .padding(start = 8.dp)
            )
        }
    }
}
