package com.example.bottomapp.ui.mainscreen

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bottomapp.R
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.BookManager
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.constants.Route
import com.example.bottomapp.ui.homescreen.HomeScreen
import com.example.bottomapp.ui.mainscreen.readerscreenproviders.IReaderScreenProvider
import com.example.bottomapp.ui.settingsscreen.SettingsScreen
import com.pdf.reader.jetpack.compose.ui.theme.PDFReaderJetpackComposeTheme
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.java.KoinJavaComponent.inject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@Composable
fun MainScreen(
    bookManager: BookManager = koinInject(), settingsManager: SettingsManager = koinInject()
) {
    val context = LocalContext.current

    val navController = rememberNavController()
    val currentTheme by settingsManager.appThemeMode.collectAsState(AppThemeMode.System)

    KoinContext() {
        PDFReaderJetpackComposeTheme(
            darkTheme = ((currentTheme == AppThemeMode.System && isSystemInDarkTheme()) || currentTheme == AppThemeMode.Dark)
        ) {
            NavHost(navController = navController,
                startDestination = Route.Main,

                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }) {
                composable(Route.Main) {
                    HomeScreen(
                        onItemClick = { bookLink ->
                            val encodedFileName = URLEncoder.encode(
                                bookLink.bundleName, StandardCharsets.UTF_8.toString()
                            )
                            navController.navigate("bookReader/$encodedFileName")
                        }, navController = navController
                    )
                }
                composable(Route.Settings) {
                    SettingsScreen(
                        navController = navController
                    )
                }
                composable(
                    "bookReader/{fileName}",
                    enterTransition = {
                        slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth })
                    },
                    exitTransition = {
                        slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
                    },
                ) { backStackEntry ->
                    val fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        URLDecoder.decode(
                            backStackEntry.arguments?.getString("fileName"), StandardCharsets.UTF_8
                        )
                    } else {
                        @Suppress("DEPRECATION") URLDecoder.decode(
                            backStackEntry.arguments?.getString("fileName")
                        )
                    }
                    fileName?.let {
                        val bookLink = bookManager.bookFlow.value.find { it.bundleName == fileName }
                        bookLink?.bookMetaInfo?.progress?.let {
                            val provider: IReaderScreenProvider = koinInject(
                                qualifier = named(bookLink.bookMetaInfo.progress.formatType),
                            )
                            provider.getReaderScreen(bookLink)()
                        }
                    }
                }
            }

            var isDialogVisible by remember { mutableStateOf(true) }
            var isNextScreenVisible by remember { mutableStateOf(false) }
            var triggerEffect by remember { mutableIntStateOf(1) }

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
                                bookManager.refreshBooksLinks()
                            }
                        }
                    } else {
                        isDialogVisible = true
                    }
                }

            if (settingsManager.getStorageUri() == null && isDialogVisible) {
                StorageSelectionDialog(onChoose = {
                    triggerEffect++
                    isDialogVisible = false
                    isNextScreenVisible = true
                })
            }

            LaunchedEffect("storage_$triggerEffect") {
                if (isNextScreenVisible) {
                    isNextScreenVisible = false
                    // Trigger directory picker in MainActivity
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    directoryLauncher.launch(intent)
                }
            }
        }
    }
}

@Composable
fun StorageSelectionDialog(
    onChoose: () -> Unit
) {
    AlertDialog(onDismissRequest = { }, title = {
        Text(
            text = getString(LocalContext.current, R.string.choose_storage),
            style = MaterialTheme.typography.headlineSmall
        )
    }, text = {
        Text(
            text = getString(LocalContext.current, R.string.choose_storage_description),
            style = MaterialTheme.typography.bodyMedium
        )
    }, confirmButton = {
        TextButton(onClick = { onChoose() }) {
            Text(text = getString(LocalContext.current, R.string.choose))
        }
    }, tonalElevation = 8.dp
    )
}

@Preview
@Composable
fun PreviewStorageSelectionDialog() {
    StorageSelectionDialog(onChoose = {})
}

@Composable
fun BottomNavigationBar(
    navController: NavController, screen: Screen
) {
    NavigationBar {
        NavigationBarItem(icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(getString(LocalContext.current, R.string.title_home)) },
            selected = screen == Screen.HOME_SCREEN,
            onClick = {
                if (navController.currentDestination?.route != Route.Main) {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.Settings) {
                            inclusive = true
                        } // Удалить все экраны до "Settings", включая "Settings"
                        launchSingleTop = true
                    }
                }
            })
        NavigationBarItem(icon = {
            Icon(
                Icons.Filled.Settings, contentDescription = null
            )
        }, label = {
            Text(
                getString(
                    LocalContext.current, R.string.title_settings
                )
            )
        },
            selected = screen == Screen.SETTINGS_SCREEN, onClick = {
                if (navController.currentDestination?.route != Route.Settings) {
                    navController.navigate(Route.Settings) {
                        launchSingleTop = true // Предотвратить дублирование "Settings" в стеке
                    }
                }
            })
    }
}