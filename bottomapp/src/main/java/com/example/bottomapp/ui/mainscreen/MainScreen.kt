package com.example.bottomapp.ui.mainscreen

import Fb2ReaderScreen
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bottomapp.R
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.BookManager
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.renderHelper.PdfRendererHelper
import com.example.bottomapp.constants.Route
import com.example.bottomapp.ui.homescreen.HomeScreen
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfReaderScreen
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfScreenViewModel
import com.example.bottomapp.ui.settingsscreen.SettingsScreen
import com.pdf.reader.jetpack.compose.ui.theme.PDFReaderJetpackComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@Composable
fun MainScreen(
    bookManager: BookManager = koinInject(),
    settingsManager: SettingsManager = koinInject()
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
                        bookManager.refreshBooksLinks()
                    }
                }
            }
        }

    val navController = rememberNavController()
    val currentTheme by settingsManager.appThemeMode.collectAsState(AppThemeMode.System)

    KoinContext() {
        PDFReaderJetpackComposeTheme(
            darkTheme = ((currentTheme == AppThemeMode.System && isSystemInDarkTheme())
                    || currentTheme == AppThemeMode.Dark)
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
                            val encodedFileName = URLEncoder.encode(
                                bookLink.bundleName,
                                StandardCharsets.UTF_8.toString()
                            )
                            navController.navigate("bookReader/$encodedFileName")
                        },
                        navController = navController
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
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth }
                        )
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth }
                        )
                    },
                ) { backStackEntry ->
                    val fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        URLDecoder.decode(
                            backStackEntry.arguments?.getString("fileName"),
                            StandardCharsets.UTF_8
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        URLDecoder.decode(
                            backStackEntry.arguments?.getString("fileName")
                        )
                    }
                    fileName?.let {
                        val bookLink = bookManager.bookFlow.value.find { it.bundleName == fileName }
                        if (bookLink?.bookMetaInfo?.progress is PdfProgress) {
                            bookLink.let {
                                val pdfBookDomainModel = PdfBookDomainModel(
                                    updateProgress = { updatedBookLink ->
                                        bookManager.updateProgress(
                                            bookLink.bundleName,
                                            updatedBookLink
                                        )
                                    },
                                    bookMetaInfo = it.bookMetaInfo,
                                    getFileFromStorage = {
                                        bookManager.getBookFile(it.bundleName)
                                            .getOrThrow()
                                    }
                                )

                                PdfReaderScreen(
                                    pdfScreenViewModel = koinViewModel<PdfScreenViewModel>(
                                        parameters = {
                                            parametersOf(
                                                pdfBookDomainModel,
                                                PdfRendererHelper { pdfBookDomainModel.getFile() }
                                            )
                                        })
                                )
                            }
                        } else if (bookLink?.bookMetaInfo?.progress is Fb2Progress) {
                            bookLink.let {
                                val book = Fb2BookDomainModel(
                                    it.bookMetaInfo,
                                    updateProgress = { updatedBookLink ->
                                        CoroutineScope(Dispatchers.IO).launch {
                                            bookManager.updateProgress(
                                                bookLink.bundleName,
                                                updatedBookLink
                                            )
                                        }
                                    },
                                    getFileFromStorage = {
                                        bookManager.getBookFile(it.bundleName)
                                            .getOrThrow()
                                    }
                                )
                                Fb2ReaderScreen(book)

                            }
                        }
                    }
                }
            }
        }
    }
    if (settingsManager.getStorageUri() == null) {
        LaunchedEffect("storage") {
            // Trigger directory picker in MainActivity
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            directoryLauncher.launch(intent)
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    screen: Screen
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
        },
            label = {
                Text(
                    getString(
                        LocalContext.current,
                        R.string.title_settings
                    )
                )
            }, // TODO еще картинку поменять
            selected = screen == Screen.SETTINGS_SCREEN,
            onClick = {
                if (navController.currentDestination?.route != Route.Settings) {
                    navController.navigate(Route.Settings) {
                        launchSingleTop = true // Предотвратить дублирование "Settings" в стеке
                    }
                }
            })
    }
}