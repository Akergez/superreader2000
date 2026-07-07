package com.akergez.superreader2000.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.ui.screens.settingsscreen.ThemeSelector
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ThemeSelectorTest {

    private val themeLabels = mapOf(
        AppThemeMode.System to "System",
        AppThemeMode.Light to "Light",
        AppThemeMode.Dark to "Dark"
    )

    @Test
    fun themeSelector_displaysAllOptions() = runDesktopComposeUiTest {
        setContent {
            MaterialTheme {
                ThemeSelector(
                    title = "App theme",
                    values = themeLabels,
                    selectedTheme = AppThemeMode.System,
                    onThemeChange = {}
                )
            }
        }

        onNodeWithText("System").assertIsDisplayed()
        onNodeWithText("Light").assertIsDisplayed()
        onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun themeSelector_onOptionClick_callsCallback() = runDesktopComposeUiTest {
        var selected: AppThemeMode? = null

        setContent {
            MaterialTheme {
                ThemeSelector(
                    title = "App theme",
                    values = themeLabels,
                    selectedTheme = AppThemeMode.System,
                    onThemeChange = { selected = it }
                )
            }
        }

        onNodeWithText("Dark").performClick()
        assertEquals(AppThemeMode.Dark, selected)
    }

    @Test
    fun themeSelector_showsTitle() = runDesktopComposeUiTest {
        setContent {
            MaterialTheme {
                ThemeSelector(
                    title = "App theme",
                    values = themeLabels,
                    selectedTheme = AppThemeMode.System,
                    onThemeChange = {}
                )
            }
        }

        onNodeWithText("App theme").assertIsDisplayed()
    }
}
