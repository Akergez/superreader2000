package com.example.bottomapp.ui.settingsscreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bottomapp.BottomNavigationBar
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.CurlMode
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    updateAppTheme: (AppThemeMode) -> Unit,
    appTheme: AppThemeMode,
    settingsManager: SettingsManager = koinInject(),
    navController: NavController
) {
    var docTheme by remember { mutableStateOf(settingsManager.getDocumentsTheme(DocThemeMode.AsIs)) } // State to control inversion
    var pdfFlipMode by remember { mutableStateOf(settingsManager.getCurlMode()) }

    val themeModeToStringMap = mapOf(
        AppThemeMode.Light to "Светлая",
        AppThemeMode.Dark to "Темная",
        AppThemeMode.System to "Системная"
    )

    Scaffold(bottomBar = { BottomNavigationBar(navController) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    RadioButton(
                        selected = docTheme == theme,
                        onClick = { docTheme = theme; settingsManager.setDocumentsTheme(docTheme) })
                    Text(text = theme.value,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable {
                                docTheme = theme; settingsManager.setDocumentsTheme(
                                docTheme
                            )
                            }
                            .padding(start = 8.dp))
                }
            }

            Text("Режим листания", style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.padding(vertical = 8.dp)) {
                arrayOf(CurlMode.PagingCurl, CurlMode.Vertical).forEach { mode ->
                    RadioButton(selected = pdfFlipMode == mode, onClick = {
                        pdfFlipMode = mode
                        settingsManager.setCurlMode(mode)
                    })
                    Text(
                        text = when (mode) {
                            CurlMode.PagingCurl -> "Классический"
                            CurlMode.Vertical -> "Вертикальный"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable {
                                pdfFlipMode = mode
                                settingsManager.setCurlMode(mode)
                            }
                            .padding(start = 8.dp))
                }
            }
        }
    }
}
