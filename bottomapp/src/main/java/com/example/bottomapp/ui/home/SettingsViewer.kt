package com.example.bottomapp.ui.home

import android.widget.RadioGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bottomapp.backend.SettingsManager

@Composable
fun SettingsScreen(
    updateAppTheme: (String) -> Unit, appTheme: String
) {
    val context = LocalContext.current
    val settingsManager = SettingsManager(context)
    var docTheme by remember { mutableStateOf(settingsManager.getDocumentsTheme("AsIs")) } // State to control inversion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("App Theme", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            listOf("Light", "Dark", "System").forEach { theme ->
                RadioButton(selected = appTheme == theme, onClick = { updateAppTheme(theme) })
                Text(text = theme,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable { updateAppTheme(theme) }
                        .padding(start = 8.dp))
            }
        }

        Text("Documents Theme", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.padding(vertical = 8.dp)) {
            listOf("AsIs", "Invert").forEach { theme ->
                RadioButton(selected = docTheme == theme, onClick = { docTheme = theme; settingsManager.setDocumentsTheme(docTheme) })
                Text(text = theme,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .clickable { docTheme = theme; settingsManager.setDocumentsTheme(docTheme) }
                        .padding(start = 8.dp))
            }
        }
    }
}
