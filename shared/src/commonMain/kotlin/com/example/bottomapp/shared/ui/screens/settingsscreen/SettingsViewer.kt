@file:OptIn(ExperimentalFoundationApi::class)

package com.example.bottomapp.shared.ui.screens.settingsscreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
public expect fun SettingsScreen(navController: Any)

@Composable
fun SettingsGroup(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .padding(vertical = 10.dp)
    )
}

@Composable
fun SettingsOption(name: String) {
    Text(name, style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 10.dp) )
}

@Composable
@Suppress("UNUSED")
fun <TEnum> ThemeSelector(
    title: String,
    values: Map<TEnum, String>,
    selectedTheme: TEnum,
    onThemeChange: (TEnum) -> Unit
) {
    SettingsOption(title)

    val options = values.values.toList()
    val selectedIndex = remember { mutableIntStateOf(options.indexOf(values[selectedTheme])) }

    SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(bottom = 15.dp)){
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = {
                    selectedIndex.intValue = index
                    val selectedValue = values.keys.toList()[index]
                    onThemeChange(selectedValue)
                },
                selected = index == selectedIndex.intValue,
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}


