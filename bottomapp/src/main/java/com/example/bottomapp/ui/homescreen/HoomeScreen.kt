package com.example.bottomapp.ui.homescreen

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bottomapp.ui.mainscreen.BottomNavigationBar
import com.example.bottomapp.backend.BookStorageEntry
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onItemClick: (BookStorageEntry) -> Unit,
    hoomeScreenViewModel: HoomeScreenViewModel = koinViewModel(),
    navController: NavController
) {

    val state by hoomeScreenViewModel.hoomeScreenState.collectAsState()
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    Log.d("color", "HomeScreen: ${MaterialTheme.colorScheme.background}")
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    hoomeScreenViewModel.addBook(uri, context)
                }
            }
        }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        val mimeTypes = arrayOf("application/pdf", "application/fb2")
                        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                    }
                    launcher.launch(intent)
                }
            )
            {
                Icon(Icons.Filled.Add, contentDescription = "Add book")
            }
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = lazyListState
        ) {
            item {
                Text(
                    text = "My Books",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            items(state.books, key = { it.fileName }) { bookLink ->
                BookItem(bookLink, onItemClick)
            }
        }
    }
}