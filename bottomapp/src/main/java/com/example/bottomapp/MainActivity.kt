package com.example.bottomapp

import PdfReaderScreen
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bottomapp.backend.AppThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.home.SettingsScreen
import com.example.bottomapp.backend.BookLink
import com.example.bottomapp.backend.BooksLinksStorage
import com.example.bottomapp.backend.PdfBookDomainModel
import com.example.bottomapp.backend.PdfProgress
import com.example.bottomapp.backend.Progress
import com.example.bottomapp.backend.StorageManager
import com.pdf.reader.jetpack.compose.ui.theme.PDFReaderJetpackComposeTheme
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.pickFile
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
        storageManager = StorageManager(this, SettingsManager(this))
        storageManager.init()

        booksLinksStorage = BooksLinksStorage(this, storageManager)
        FileKit.init(this)
        setContent {
            MainScreen()
        }
    }

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
            }
        }
        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                val fileName = getFileName(uri)
                storageManager.copyBookToStorage(
                    uri,
                    BookLink(fileName, fileName, PdfProgress(0)),
                    contentResolver
                )
                booksLinksStorage.upsertBook(BookLink(fileName, fileName, PdfProgress(0)))
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
            type = "application/pdf"
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

        PDFReaderJetpackComposeTheme(darkTheme = ((currentTheme.value == AppThemeMode.System && isSystemInDarkTheme()) || currentTheme.value == AppThemeMode.Dark)) {
            Scaffold(bottomBar = { BottomNavigationBar(navController) }) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("home",
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it }
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it / 2 }
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it }
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it / 2 }
                            )
                        }
                    ) {
                        HomeScreen(
                            booksLinks = booksLinksStorage.booksLinks,
                            onAddPdf = {
                                lifecycleScope.launch { openFilePicker() }
                            },
                            onItemClick = { bookLink ->
                                navController.navigate(
                                    "pdfReader/${
                                        URLEncoder.encode(
                                            bookLink.fileName,
                                            StandardCharsets.UTF_8.toString()
                                        )
                                    }"
                                )
                            }
                        )
                    }
                    composable(settingsKey,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it }
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it / 2 }
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it }
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it / 2 }
                            )
                        }

                    ) {
                        SettingsScreen(
                            updateAppTheme = { theme ->
                                currentTheme.value = theme
                                settingsManager.setAppTheme(theme)
                            },
                            appTheme = currentTheme.value
                        )
                    }
                    composable("pdfReader/{fileName}",
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it }
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it / 2 }
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it }
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it / 2 }
                            )
                        }
                    ) { backStackEntry ->
                        val fileName = backStackEntry.arguments?.getString("fileName")
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
                }
            }
        }
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

    @Composable
    fun HomeScreen(
        booksLinks: List<BookLink>,
        onAddPdf: () -> Unit,
        onItemClick: (BookLink) -> Unit
    ) {
        val rememberedBooksLinks = remember { booksLinks }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "My Books",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                items(rememberedBooksLinks) { bookLink ->
                    BookItem(bookLink, onItemClick)
                }
            }
            FloatingActionButton(
                onClick = onAddPdf,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add PDF")
            }
        }
    }

    @Composable
    fun BookItem(bookLink: BookLink, onItemClick: (BookLink) -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onItemClick(bookLink) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(modifier = Modifier.padding(8.dp)) {
                    val previewBitmap = remember {
                        storageManager.getBookFile(bookLink.fileName).getPreviewBitmap()
                    }
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color.White)
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Column {
                        Text(
                            text = bookLink.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    // Extension function to get a preview bitmap from a PDF file
    private fun File.getPreviewBitmap(): Bitmap? {
        var pdfRenderer: PdfRenderer? = null
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        try {
            parcelFileDescriptor =
                ParcelFileDescriptor.open(this, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                val width = page.width
                val height = page.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                return bitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    @Composable
    fun DashboardScreen() {
        // Your Dashboard Screen content
    }

    @Composable
    fun NotificationsScreen() {
        // Your Notifications Screen content
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        PDFReaderJetpackComposeTheme() {
            MainScreen()
        }
    }
}