package com.example.bottomapp.ui.settingsscreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.pdfscreen.pdfflippings.FlipMode
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    updateAppTheme: (AppThemeMode) -> Unit,
    appTheme: AppThemeMode,
    settingsManager: SettingsManager = koinInject()
) {
    var docTheme by remember { mutableStateOf(settingsManager.getDocumentsTheme(DocThemeMode.AsIs)) } // State to control inversion
    var pdfFlipMode by remember { mutableStateOf(settingsManager.getPdfFlipMode()) }

    val themeModeToStringMap = mapOf(
        AppThemeMode.Light to "Светлая",
        AppThemeMode.Dark to "Темная",
        AppThemeMode.System to "Системная"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("App Theme", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            AppThemeMode.entries.forEach { theme ->
                RadioButton(selected = appTheme == theme, onClick = { updateAppTheme(theme) })
                Text(text = themeModeToStringMap[theme] ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable { updateAppTheme(theme) }
                        .padding(start = 8.dp))
            }
        }

        Text("Documents Theme", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            DocThemeMode.entries.forEach { theme ->
                RadioButton(selected = docTheme == theme, onClick = { docTheme = theme; settingsManager.setDocumentsTheme(docTheme) })
                Text(text = theme.value,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable { docTheme = theme; settingsManager.setDocumentsTheme(docTheme) }
                        .padding(start = 8.dp))
            }
        }

        Text("Режим листания", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            FlipMode.entries.forEach { mode ->
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
    }
}
