package com.example.bottomapp.shared.ui.screens.mainscreen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.bottomapp.shared.backend.BookManager
import com.example.bottomapp.shared.backend.settings.AppThemeMode
import com.example.bottomapp.shared.backend.settings.ISettingsManager
import com.example.bottomapp.shared.constants.Route
import com.example.bottomapp.shared.ui.screens.Screen
import com.example.bottomapp.shared.ui.screens.homescreen.HomeScreen
import com.example.bottomapp.shared.formats.abstractions.IReaderScreenProvider
import com.example.bottomapp.shared.ui.screens.settingsscreen.SettingsScreen
import com.example.bottomapp.shared.ui.theme.AppTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import pdfreaderjetpackcompose.shared.generated.resources.Res
import pdfreaderjetpackcompose.shared.generated.resources.choose
import pdfreaderjetpackcompose.shared.generated.resources.choose_storage
import pdfreaderjetpackcompose.shared.generated.resources.choose_storage_description

// Platform-specific expect declarations
@Composable
expect fun rememberStorageDirectoryPicker(
    onDirectorySelected: (String) -> Unit,
    onPermissionGranted: suspend () -> Unit
): StorageDirectoryPicker

expect class StorageDirectoryPicker {
    fun launch()
}

// URL encoding/decoding expect functions
expect fun urlEncode(value: String): String
expect fun urlDecode(value: String): String

@Serializable
data class BookReader(val fileName: String)

@Composable
fun MainScreen(
    bookManager: BookManager = koinInject(),
    settingsManager: ISettingsManager = koinInject()
) {
    val navController = rememberNavController()
    val currentTheme by settingsManager.appThemeModeFlow.collectAsState()

    AppTheme(
        darkTheme = ((currentTheme == AppThemeMode.System && isSystemInDarkTheme()) || currentTheme == AppThemeMode.Dark)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = Route.Main,
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                composable(Route.Main) {
                    HomeScreen(
                        onItemClick = { bookLink ->
                            val encodedFileName = urlEncode(bookLink.bundleName)
                            navController.navigate(BookReader(encodedFileName))
                        },
                        bottomBar = {
                            com.example.bottomapp.shared.ui.components.BottomNavigationBar(
                                navController,
                                Screen.HOME_SCREEN
                            )
                        }
                    )
                }
                composable(Route.Settings) {
                    SettingsScreen(
                        navController = navController
                    )
                }
                composable<BookReader>(
                    enterTransition = {
                        slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
                    },
                    exitTransition = {
                        slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
                    },
                ) { backStackEntry ->
                    val encodedFileName = backStackEntry.toRoute<BookReader>().fileName
                    encodedFileName.let {
                        val fileName = urlDecode(it)
                        val bookLink = bookManager.bookFlow.value.find { it.bundleName == fileName }
                        bookLink?.bookMetaInfo?.progress?.let { progress ->
                            val provider: IReaderScreenProvider = koinInject(
                                qualifier = named(progress.formatType),
                            )
                            provider.getReaderScreen(bookLink)()
                        }
                    }
                }
            }
        }

        // Storage selection dialog and picker
        var isDialogVisible by remember { mutableStateOf(true) }
        var isNextScreenVisible by remember { mutableStateOf(false) }
        var triggerEffect by remember { mutableIntStateOf(1) }

        val directoryPicker = rememberStorageDirectoryPicker(
            onDirectorySelected = { uri ->
                settingsManager.setStoragePath(uri)
            },
            onPermissionGranted = {
                bookManager.refreshBooksLinks()
            }
        )

        if (settingsManager.getStoragePath() == null && isDialogVisible) {
            StorageSelectionDialog(onChoose = {
                triggerEffect++
                isDialogVisible = false
                isNextScreenVisible = true
            })
        }

        LaunchedEffect("storage_$triggerEffect") {
            if (isNextScreenVisible) {
                isNextScreenVisible = false
                directoryPicker.launch()
            }
        }
    }
}

@Composable
fun StorageSelectionDialog(
    onChoose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = stringResource(Res.string.choose_storage),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.choose_storage_description),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = { onChoose() }) {
                Text(text = stringResource(Res.string.choose))
            }
        },
        tonalElevation = 8.dp
    )
}

