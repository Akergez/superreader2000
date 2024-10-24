package com.example.bottomapp

import PdfReaderScreen
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.settingsscreen.SettingsScreen
import com.example.bottomapp.backend.BookLink
import com.example.bottomapp.backend.BooksLinksStorage
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.StorageManager
import com.example.bottomapp.ui.homescreen.HomeScreen
import com.example.bottomapp.ui.home.Fb2ReaderScreen
import com.pdf.reader.jetpack.compose.ui.theme.PDFReaderJetpackComposeTheme
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {
    private val settingsKey = "Settings"

    private lateinit var booksLinksStorage: BooksLinksStorage
    private lateinit var storageManager: StorageManager
    private val PICK_PDF_FILE = 2
    private val PICK_DIRECTORY_REQUEST_CODE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Initialize essential components
        storageManager = StorageManager(this, SettingsManager(this))


        // Delay non-essential initializations
        booksLinksStorage = BooksLinksStorage(this@MainActivity, storageManager)
        lifecycleScope.launch {
            storageManager.init()
            FileKit.init(this@MainActivity)
            booksLinksStorage.refreshBooksLinks()
        }

        setContent {
            MainScreen()
        }
    }

    // Other methods remain unchanged

    fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, PICK_DIRECTORY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val settingsManager = SettingsManager(this)
                settingsManager.saveStorageUri(uri)
                lifecycleScope.launch {
                    booksLinksStorage.refreshBooksLinks()
                }
            }
        }
        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                val fileName = getFileName(uri)
                val fileType = getFileType(fileName)
                val bookLink = when (fileType) {
                    "pdf" -> BookLink(fileName, fileName, PdfProgress(0))
                    "fb2" -> BookLink(fileName, fileName, Fb2Progress(0))
                    else -> return@also
                }
                storageManager.copyBookToStorage(uri, bookLink, contentResolver)
                booksLinksStorage.upsertBook(bookLink)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        booksLinksStorage.saveBooksLinksToStorage()
        storageManager.clearFileCache()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "application/fb2"))

        }
        startActivityForResult(intent, PICK_PDF_FILE)
    }

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
    fun MainScreen() {
        val navController = rememberNavController()
        val settingsManager = SettingsManager(this)
        val currentTheme = remember {
            mutableStateOf(settingsManager
                    .getAppTheme(AppThemeMode.System))
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
                                lifecycleScope.launch { openFilePicker() }
                            },
                            onItemClick = { bookLink ->
                                val encodedFileName = URLEncoder.encode(bookLink.fileName, StandardCharsets.UTF_8.toString())
                                when (getFileType(bookLink.fileName)) {
                                    "pdf" -> navController.navigate("pdfReader/$encodedFileName")
                                    "fb2" -> navController.navigate("fb2Reader/$encodedFileName")
                                }
                            },
                            storageManager = storageManager,
                            booksLinksStorage = booksLinksStorage
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
                                booksLinksStorage.booksLinks.find { it.fileName == fileName }
                            bookLink?.let {
                                val pdfBookDomainModel = PdfBookDomainModel(
                                    it,
                                    this@MainActivity,
                                    storageManager = storageManager,
                                    updateProgress = { updatedBookLink ->
                                        booksLinksStorage.upsertBook(updatedBookLink)
                                    }
                                )
                                PdfReaderScreen(pdfBookDomainModel, settingsManager)
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