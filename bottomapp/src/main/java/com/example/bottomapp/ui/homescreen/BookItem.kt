package com.example.bottomapp.ui.homescreen

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bottomapp.backend.BookLink
import com.example.bottomapp.backend.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BookItem(bookLink: BookLink,
             onItemClick: (BookLink) -> Unit,
             storageManager: StorageManager
) {
    val previewUri = remember(bookLink.fileName) {
        mutableStateOf<Uri?>(null)
    }
    LaunchedEffect(bookLink.fileName) {
        previewUri.value = withContext(Dispatchers.IO) {
            storageManager.getPreviewUri(bookLink.fileName)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onItemClick(bookLink) }
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                uri = previewUri.value,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = bookLink.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}