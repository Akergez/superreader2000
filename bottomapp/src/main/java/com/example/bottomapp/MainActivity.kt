package com.example.bottomapp

import PdfReaderScreen
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bottomapp.databinding.ActivityMainBinding
import com.pdf.reader.jetpack.compose.ui.theme.PDFReaderJetpackComposeTheme
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val pdfFiles = mutableStateListOf<PdfFile>()
    private val PICK_PDF_FILE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContent {
            PDFReaderJetpackComposeTheme {
                MainScreen()
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, PICK_PDF_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                val fileName = getFileName(uri)
                val file = File(cacheDir, fileName)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                pdfFiles.add(PdfFile(fileName, file))
            }
        }
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
        Scaffold(bottomBar = {
            BottomNavigationBar(navController)
        }) { innerPadding ->
            NavHost(
                navController, startDestination = "home", modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    HomeScreen(
                        pdfFiles = pdfFiles,
                        onAddPdf = { openFilePicker() },
                        onItemClick = { pdfFile ->
                            navController.navigate("pdfReader/${URLEncoder.encode(pdfFile.file.path, StandardCharsets.UTF_8.toString())}")
                        }
                    )
                }
                composable("dashboard") { DashboardScreen() }
                composable("notifications") { NotificationsScreen() }
                composable("pdfReader/{filePath}") { backStackEntry ->
                    val filePath = backStackEntry.arguments?.getString("filePath")
                    filePath?.let {
                        PdfReaderScreen(File(it))
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
            NavigationBarItem(icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                label = { Text("Dashboard") },
                selected = false,
                onClick = { navController.navigate("dashboard") })
            NavigationBarItem(icon = {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null
                )
            },
                label = { Text("Notifications") },
                selected = false,
                onClick = { navController.navigate("notifications") })
        }
    }

    data class PdfFile(val name: String, val file: File)

    @Composable
    fun HomeScreen(pdfFiles: List<PdfFile>, onAddPdf: () -> Unit, onItemClick: (PdfFile) -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "My Books",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                items(pdfFiles) { pdfFile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onItemClick(pdfFile) }
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            val previewBitmap = pdfFile.file.getPreviewBitmap()
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(end = 8.dp)
                                )
                            }
                            Text(
                                text = pdfFile.name,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
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

    // Extension function to get a preview bitmap from a PDF file
    fun File.getPreviewBitmap(): Bitmap? {
        // Implement logic to generate a preview bitmap from the PDF file
        // This is a placeholder implementation
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