package com.example.bottomapp.ui.homescreen

import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bottomapp.BottomNavigationBar
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.backend.BookStorageEntry
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    onAddPdf: () -> Unit,
    onItemClick: (BookStorageEntry) -> Unit,
    bookStorage: BookStorage = koinInject(),
    navController: NavController
) {

    val updatedBooksLinks by rememberUpdatedState(bookStorage.bookCache)
    val lazyListState = rememberLazyListState()
    Log.d("color", "HomeScreen: ${MaterialTheme.colorScheme.background}")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPdf
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
            items(updatedBooksLinks, key = { it.fileName }) { bookLink ->
                BookItem(bookLink, onItemClick)
            }
        }
    }

    LaunchedEffect(Unit) {
        bookStorage.saveBooksLinksToStorage()
    }
}