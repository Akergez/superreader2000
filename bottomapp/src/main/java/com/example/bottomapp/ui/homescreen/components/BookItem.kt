package com.example.bottomapp.ui.homescreen.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bottomapp.backend.BookStorageEntry

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookItem(
    book: BookStorageEntry,
    onItemClick: (BookStorageEntry) -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean
) {
    val shape = RoundedCornerShape(8.dp)

    ListItem(
        leadingContent = {
            AsyncImage(
                uri = book.previewUri,
                modifier = Modifier
                    .size(100.dp, 150.dp)
                    .clip(shape)
            )
        },
        headlineContent = {
            Text(
                text = book.bookMetaInfo.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = book.bookMetaInfo.author ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = if (book.progressInPercent != null) book.progressInPercent.toInt().toString() + "%" else "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )

            }
        },
        modifier = Modifier
            .combinedClickable(
                onClick = { onItemClick(book) },
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.onBackground,
                    shape
                ) else Modifier
            )
    )
}