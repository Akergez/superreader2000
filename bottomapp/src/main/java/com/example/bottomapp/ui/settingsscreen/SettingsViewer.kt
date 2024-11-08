@file:OptIn(ExperimentalFoundationApi::class)

package com.example.bottomapp.ui.settingsscreen

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.SettingsManager
import kotlinx.coroutines.launch
import com.example.bottomapp.ui.readerscreens.common.FlipMode
import org.koin.compose.koinInject


@Preview(showBackground = true)
@Composable
fun DefaultSettingsScreen() {
    SettingsScreen({ theme -> },
        AppThemeMode.Light,
        SettingsManager(LocalContext.current)
    )
}


@Composable
fun SettingsScreen(
    updateAppTheme: (AppThemeMode) -> Unit,
    appTheme: AppThemeMode,
    settingsManager: SettingsManager = koinInject()
) {
    val context = LocalContext.current
    var docTheme by remember { mutableStateOf(settingsManager.getDocumentsTheme(DocThemeMode.AsIs)) } // State to control inversion
    var pdfFlipMode by remember { mutableStateOf(settingsManager.getPdfFlipMode()) }
    var showSecondComposable by remember { mutableStateOf(false) }

    var storageAddress by remember { mutableStateOf(settingsManager.getStorageUri().toString()) }


    if (showSecondComposable) {
        ChangeStorageAddress(settingsManager = settingsManager,
            onResult = {
                status, newStorageAddress ->
            showSecondComposable = status
            storageAddress = newStorageAddress
        })
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("App theme", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            AppThemeMode.entries.forEach { theme ->
                RadioButton(selected = appTheme == theme, onClick = { updateAppTheme(theme) })
                Text(text = when (theme) {
                    AppThemeMode.Light -> "Light"
                    AppThemeMode.Dark -> "Dark"
                    AppThemeMode.System -> "System"
                },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable { updateAppTheme(theme) }
                        .padding(start = 8.dp))
            }
        }

        Text("Documents theme", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            DocThemeMode.entries.forEach { theme ->
                RadioButton(
                    selected = docTheme == theme,
                    onClick = {
                        docTheme = theme
                        settingsManager.setDocumentsTheme(docTheme)
                    })
                Text(text = theme.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable {
                            docTheme = theme
                            settingsManager.setDocumentsTheme(docTheme)
                        }
                        .padding(start = 8.dp))
            }
        }

        Row(Modifier.padding(32.dp)) {

        }

        Text("Paging mode", style = MaterialTheme.typography.headlineMedium)
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
                            showSecondComposable = true
                            // это вызывает обновление экрана
                        },
                        onLongClick = {
                            val longPressText = "#Trump2024"
                            Toast
                                .makeText(context, longPressText, Toast.LENGTH_LONG)
                                .show()
                            storageAddress = settingsManager
                                .getStorageUri()
                                .toString()
                        },
                    )
                    .padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun ChangeStorageAddress(
    settingsManager: SettingsManager,
    onResult: (Boolean, String) -> Unit
) {
    val context = LocalContext.current

    val directoryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    settingsManager.saveStorageUri(uri)
                    (context as ComponentActivity).lifecycleScope.launch {
                    }
                    onResult(false, uri.toString())
                }
            }
        }

    LaunchedEffect("storage") {
        // Trigger directory picker in MainActivity
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        directoryLauncher.launch(intent)
    }
}
