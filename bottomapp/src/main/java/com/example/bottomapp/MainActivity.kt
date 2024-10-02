package com.example.bottomapp

import PdfReaderScreen
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bottomapp.databinding.ActivityMainBinding
import com.pdf.reader.jetpack.compose.ui.theme.PDFReaderJetpackComposeTheme
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContent {
            PDFReaderJetpackComposeTheme {
                MainScreen()
            }
        }
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
                composable("home") { HomeScreen() }
                composable("dashboard") { DashboardScreen() }
                composable("notifications") { NotificationsScreen() }
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

    @Composable
    fun HomeScreen() {
        val pdfFile = File(filesDir, "gold.pdf")
        //val pdfRendererHelper = PdfRendererHelper(this, pdfFile)
        PdfReaderScreen()
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