package com.akergez.superreader2000.shared.ui.screens.mainscreen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.settings.AppThemeMode
import com.akergez.superreader2000.shared.backend.settings.DocThemeMode
import com.akergez.superreader2000.shared.backend.settings.ISettingsManager
import com.akergez.superreader2000.shared.constants.Route
import com.akergez.superreader2000.shared.ui.components.AppNavigationRail
import com.akergez.superreader2000.shared.ui.components.BottomNavigationBar
import com.akergez.superreader2000.shared.ui.screens.Screen
import com.akergez.superreader2000.shared.ui.screens.homescreen.HomeScreen
import com.akergez.superreader2000.shared.formats.abstractions.IReaderScreenProvider
import com.akergez.superreader2000.shared.ui.screens.settingsscreen.SettingsScreen
import com.akergez.superreader2000.shared.ui.screens.settingsscreen.SyncSettingsScreen
import com.akergez.superreader2000.shared.ui.theme.AppTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.choose
import superreader2000.shared.generated.resources.choose_storage
import superreader2000.shared.generated.resources.choose_storage_description

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

@Composable
expect fun ReaderImmersiveMode(enabled: Boolean)

@Serializable
data class BookReader(val fileName: String)

@Composable
fun MainScreen(
    bookManager: BookManager = koinInject(),
    settingsManager: ISettingsManager = koinInject()
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isReaderScreen = currentRoute != null &&
        currentRoute != Route.Main &&
        currentRoute != Route.Settings &&
        currentRoute != Route.SyncSettings
    ReaderImmersiveMode(isReaderScreen)
    val currentScreen = when (currentRoute) {
        Route.Main -> Screen.HOME_SCREEN
        Route.Settings -> Screen.SETTINGS_SCREEN
        Route.SyncSettings -> Screen.SYNC_SCREEN
        else -> Screen.HOME_SCREEN
    }
    val currentTheme by settingsManager.appThemeModeFlow.collectAsState()
    val readerBackground = when (settingsManager.getFixedDocumentsTheme(DocThemeMode.AsIs)) {
        DocThemeMode.Invert -> Color.Black
        DocThemeMode.AsIs -> Color.White
        DocThemeMode.SoftLight -> Color(0xFFD8D6D1)
        DocThemeMode.SoftDark -> Color(0xFF242424)
        DocThemeMode.MaterialYou -> Color.Transparent
    }

    AppTheme(
        darkTheme = ((currentTheme == AppThemeMode.System && isSystemInDarkTheme()) || currentTheme == AppThemeMode.Dark)
    ) {
        Surface(
            color = if (isReaderScreen && readerBackground != Color.Transparent) {
                readerBackground
            } else {
                MaterialTheme.colorScheme.background
            }
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val widthDp = with(density) { constraints.maxWidth.toDp() }
                val heightDp = with(density) { constraints.maxHeight.toDp() }
                val isLandscape = widthDp > heightDp
                val useRail = isLandscape && widthDp >= 600.dp
                val showRail = useRail && (currentRoute == Route.Main || currentRoute == Route.Settings || currentRoute == Route.SyncSettings)

                Row(Modifier.fillMaxSize()) {
                    if (showRail) {
                        AppNavigationRail(navController, currentScreen)
                    }

                    NavHost(
                        modifier = Modifier.weight(1f),
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
                                    if (!useRail) {
                                        BottomNavigationBar(
                                            navController,
                                            Screen.HOME_SCREEN
                                        )
                                    }
                                }
                            )
                        }
                        composable(Route.Settings) {
                            SettingsScreen(
                                navController = navController,
                                bottomBar = {
                                    if (!useRail) {
                                        BottomNavigationBar(
                                            navController,
                                            Screen.SETTINGS_SCREEN
                                        )
                                    }
                                }
                            )
                        }
                        composable(Route.SyncSettings) {
                            SyncSettingsScreen(
                                bottomBar = {
                                    if (!useRail) BottomNavigationBar(navController, Screen.SYNC_SCREEN)
                                }
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
                                    // Экран владеет состоянием сессии чтения и должен быть
                                    // стабилен между рекомпозициями одной записи навигации.
                                    val readerScreen = remember(provider, bookLink.bundleName) {
                                        provider.getReaderScreen(bookLink)
                                    }
                                    readerScreen()
                                }
                            }
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
