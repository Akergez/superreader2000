package com.example.bottomapp

import Fb2ReaderScreen
import PdfReaderScreen
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.settingsscreen.SettingsScreen
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.BookMetaInfo
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.StorageManager
import com.example.bottomapp.ui.homescreen.HomeScreen
import com.pdf.reader.jetpack.compose.ui.theme.PDFReaderJetpackComposeTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private val settingsKey = "Settings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appModule = module {
            singleOf(::StorageManager) bind StorageManager::class
            singleOf(::BookStorage) bind BookStorage::class
            singleOf(::SettingsManager) bind SettingsManager::class
            single {
                ImageLoader.Builder(androidContext())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache {
                        MemoryCache.Builder(androidContext()).maxSizePercent(0.25).build()
                    } // 25% of app's memory
                    .build()
            }
        }

        startKoin {
            androidLogger()
            androidContext(this@MainActivity)
            modules(appModule)
        }
        setContent {
            MainScreen()
        }
    }

    //ToDo починить
    /*override fun onDestroy() {
        super.onDestroy()
        booksLinksStorage.saveBooksLinksToStorage()
        storageManager.clearFileCache()
    }*/

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
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
                        result.data?.data?.also { uri ->val fileName = getFileName(uri)
                        when (val fileType = getFileType(fileName)) {
                                "pdf", "fb2" -> {
                                    val bookLink = when (fileType) {
                                        "pdf" -> BookLink(fileName, fileName, PdfProgress(0))
                                        "fb2" -> BookLink(fileName, fileName, Fb2Progress(0))
                                        else -> return@also // This should never happen due to the when condition
                                    }
                                    storageManager.copyBookToStorage(uri, bookLink, contentResolver)
                                    booksLinksStorage.upsertBook(bookLink)
                                }
                                else -> {
                                    Toast.makeText(
                                        this,
                                        "Только файлы формата PDF и FB2 поддерживаются приложением",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    lifecycleScope.launch { openFilePicker() }
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
                        lifecycleScope.launch {
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
                    startDestination = "home",
                    modifier = Modifier.padding(innerPadding),
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it }
                        )
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it }
                        )
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it / 2 }
                        )
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it / 2 }
                        )
                    }
                ) {
                    composable("home") {
                        HomeScreen(
                            onAddPdf = {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/pdf"
                                }
                                launcher.launch(intent)
                            },
                            onItemClick = { bookLink ->
                                val encodedFileName = URLEncoder.encode(bookLink.fileName, StandardCharsets.UTF_8.toString())
                                when (getFileType(bookLink.fileName)) {
                                    "pdf" -> navController.navigate("pdfReader/$encodedFileName")
                                    "fb2" -> navController.navigate("fb2Reader/$encodedFileName")
                                }
                            }
                        )
                    }
                    composable(settingsKey) {
                        SettingsScreen(
                            updateAppTheme = { theme ->
                                currentTheme.value = theme
                                settingsManager.setAppTheme(theme)
                            },
                            appTheme = currentTheme.value
                        )
                    }
                    composable("pdfReader/{fileName}") { backStackEntry ->
                        val fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            URLDecoder.decode(
                                backStackEntry.arguments?.getString("fileName"),
                                StandardCharsets.UTF_8
                            )
                        } else {
                            URLDecoder.decode(
                                backStackEntry.arguments?.getString("fileName")
                            )
                        }
                        fileName?.let {
                            val bookLink =
                                bookStorage.bookCache.find { it.fileName == fileName }
                            bookLink?.let {
                                val pdfBookDomainModel = PdfBookDomainModel(
                                    it.bookMetaInfo,
                                    context as ComponentActivity,
                                    storageManager = storageManager,
                                    updateProgress = { updatedBookLink ->
                                        bookStorage.updateProgress(updatedBookLink)
                                    }
                                )
                                PdfReaderScreen(pdfBookDomainModel)
                            }
                        }
                    }
                    composable("fb2Reader/{fileName}") { backStackEntry ->
                        val fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            URLDecoder.decode(
                                backStackEntry.arguments?.getString("fileName"),
                                StandardCharsets.UTF_8
                            )
                        } else {
                            URLDecoder.decode(
                                backStackEntry.arguments?.getString("fileName")
                            )
                        }
                        fileName?.let {
                            val bookLink =
                                booksLinksStorage.booksLinks.find { it.fileName == fileName }
                            bookLink?.let {
                                val book = Fb2BookDomainModel(
                                    it,
                                    this@MainActivity,
                                    storageManager = storageManager,
                                    updateProgress = { updatedBookLink ->
                                        booksLinksStorage.upsertBook(updatedBookLink)
                                    }
                                )
                                Fb2ReaderScreen(book, settingsManager)
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(settingsManager.getStorageUri() == null) {
            // Trigger directory picker in MainActivity
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            directoryLauncher.launch(intent)
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
                onClick = { navController.navigate("home") })
            NavigationBarItem(icon = {
                Icon(
                    Icons.Filled.Settings, contentDescription = null
                )
            },
                label = { Text("Settings") }, // TODO еще картинку поменять
                selected = false,
                onClick = { navController.navigate(settingsKey) })
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        PDFReaderJetpackComposeTheme() {
            MainScreen()
        }
    }

    private fun getFileType(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }
}