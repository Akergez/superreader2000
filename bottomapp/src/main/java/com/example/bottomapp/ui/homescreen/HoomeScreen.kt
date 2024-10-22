package com.example.bottomapp.ui.homescreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bottomapp.backend.BookLink
import com.example.bottomapp.backend.BooksLinksStorage
import com.example.bottomapp.backend.StorageManager

@Composable
fun HomeScreen(
    onAddPdf: () -> Unit,
    onItemClick: (BookLink) -> Unit,
    booksLinksStorage: BooksLinksStorage,
    storageManager: StorageManager
) {

    val updatedBooksLinks by rememberUpdatedState(booksLinksStorage.booksLinks)

    LaunchedEffect(Unit) {
        booksLinksStorage.saveBooksLinksToStorage()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = "My Books",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            items(updatedBooksLinks, key = { it.fileName }) { bookLink ->
                BookItem(bookLink, onItemClick, storageManager)
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