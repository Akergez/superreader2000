package com.example.bottomapp.ui.readerscreens.common.bookFlippings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.BookPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PageCarousel(
    bookContent: (Int) -> MutableStateFlow<IBookContent>,
    totalPages: MutableStateFlow<Int>,
    onPageChange: (Int) -> Unit,
    onSettingsClick: () -> Job,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val lazyListState = rememberLazyListState(initialPage)
    val totalPagesState by totalPages.collectAsState()

    Column(modifier = modifier) {
        LazyRow(
            modifier = Modifier.weight(0.9f),
            state = lazyListState,
        ) {
            items(totalPagesState, key = { it }) { page ->
                Box(
                    Modifier
                        .fillParentMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Card(
                        Modifier
                            .fillParentMaxWidth(0.9f)
                            .align(Alignment.Center)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(8.dp),
                                clip = true
                            )
                    ) {
                        BookPage(
                            bookContent = bookContent(page),
                            applyInversion = applyInversion
                        )
                    }
                }
            }
        }

        val firstVisibleItemIndex by remember {
            derivedStateOf { lazyListState.firstVisibleItemIndex }
        }

        LaunchedEffect(firstVisibleItemIndex) {
            onPageChange(firstVisibleItemIndex)
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page ${firstVisibleItemIndex + 1} of $totalPagesState",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
//            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { onSettingsClick() }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}
