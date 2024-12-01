package com.example.bottomapp

import Fb2ReaderScreen
import com.example.bottomapp.ui.readerscreens.pdfscreen.PdfReaderScreen
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.backend.StorageManager
import com.example.bottomapp.constants.FileType
import com.example.bottomapp.constants.Route
import com.example.bottomapp.ui.homescreen.HomeScreen
import com.example.bottomapp.ui.settingsscreen.SettingsScreen
import com.pdf.reader.jetpack.compose.ui.theme.PDFReaderJetpackComposeTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor.use {
            if (it != null && it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = it.getString(nameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "unknown"
}

private fun getFileType(fileName: String): FileType {
    val strType = fileName.substringAfterLast('.', "").lowercase()
    val fileTypeEnum: FileType = try { enumValueOf<FileType>(strType)
    } catch (e: IllegalArgumentException) {
        FileType.unknown
    }
    return fileTypeEnum
}

@Composable
fun MainScreen(
    storageManager: StorageManager = koinInject(),
    bookStorage: BookStorage = koinInject()
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    val fileName = getFileName(context, uri)
                    when (val fileType = getFileType(fileName)) {
                        FileType.fb2 -> {
                            val bookLink = Fb2Progress(0)
                            bookStorage.addNewBook(uri, fileName, bookLink, fileType)
                        }
                        FileType.pdf -> {
                            val bookLink = PdfProgress(0)
                            bookStorage.addNewBook(uri, fileName, bookLink, fileType)
                        }
                        FileType.unknown -> {
                            Toast.makeText(
                                context,
                                "Только файлы формата PDF и FB2 поддерживаются приложением",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

    val directoryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    val settingsManager = SettingsManager(context)
                    settingsManager.saveStorageUri(uri)
                    (context as ComponentActivity).lifecycleScope.launch {
                        bookStorage.refreshBooksLinks()
                    }
                }
            }
        }

    val navController = rememberNavController()
    val settingsManager = SettingsManager(context)
    val currentTheme = remember {
        mutableStateOf(
            settingsManager.getAppTheme(AppThemeMode.System)
        )
    }

    PDFReaderJetpackComposeTheme(
        darkTheme = ((currentTheme.value == AppThemeMode.System && isSystemInDarkTheme())
                || currentTheme.value == AppThemeMode.Dark)
    ) {
        Scaffold(
            bottomBar = {
                if (shouldShowBottomBar(navController)) {
                    BottomNavigationBar(navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Route.Main,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                composable(Route.Main) {
                    HomeScreen(
                        onAddBook = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                val mimeTypes = arrayOf("application/pdf", "application/fb2")
                                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                            }
                            launcher.launch(intent)
                        },
                        onItemClick = { bookLink ->
                            val encodedFileName = URLEncoder.encode(
                                bookLink.fileName,
                                StandardCharsets.UTF_8.toString()
                            )
                            navController.navigate("bookReader/$encodedFileName")
                        }
                    )
                }
                composable(Route.Settings) {
                    SettingsScreen(
                        updateAppTheme = { theme ->
                            currentTheme.value = theme
                            settingsManager.setAppTheme(theme)
                        },
                        appTheme = currentTheme.value
                    )
                }
                composable("bookReader/{fileName}",
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
                        val bookLink = bookStorage.bookCache.find { it.fileName == fileName }
                        if (bookLink?.bookMetaInfo?.progress is PdfProgress) {
                            bookLink.let {
                                val pdfBookDomainModel = PdfBookDomainModel(
                                    it.bookMetaInfo,
                                    context,
                                    storageManager = storageManager,
                                    updateProgress = { updatedBookLink ->
                                        bookStorage.updateProgress(updatedBookLink)
                                    }
                                )
                                PdfReaderScreen(pdfBookDomainModel)
                            }
                        } else if (bookLink?.bookMetaInfo?.progress is Fb2Progress) {
                            bookLink.let {
                                val book = Fb2BookDomainModel(
                                    it.bookMetaInfo,
                                    context,
                                    storageManager = storageManager,
                                    updateProgress = { updatedBookLink ->
                                        bookStorage.updateProgress(updatedBookLink)
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
fun shouldShowBottomBar(navController: NavController): Boolean {
    val currentDestination =
        navController.currentBackStackEntryAsState().value?.destination?.route
    return currentDestination != null && currentDestination != "pdfReader/{fileName}"
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = false,
            onClick = {
                if (navController.currentDestination?.route != Route.Main) {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.Settings) { inclusive = true } // Удалить все экраны до "Settings", включая "Settings"
                        launchSingleTop = true
                    }
                }
            })
        NavigationBarItem(icon = {
            Icon(
                Icons.Filled.Settings, contentDescription = null
            )
        },
            label = { Text("Settings") }, // TODO еще картинку поменять
            selected = false,
            onClick = {
                if (navController.currentDestination?.route != Route.Settings) {
                    navController.navigate(Route.Settings){
                    launchSingleTop = true // Предотвратить дублирование "Settings" в стеке
                    }
                }
            })
    }
}