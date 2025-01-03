package com.example.bottomapp.ui.readerscreens.common.bookFlippings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.BookPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun PageCarousel(
    bookContent: (Int) -> MutableStateFlow<IBookContent>,
    totalPages: MutableStateFlow<Int>,
    onPageChange: (Int) -> Unit,
    onSettingsClick: () -> Job,
    onBack: () -> Unit,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    title: String
) {
    val lazyListState = rememberLazyListState(initialPage)
    val totalPagesState by totalPages.collectAsState()


    Column(modifier = modifier) {
        Row (verticalAlignment = Alignment.CenterVertically){
            IconButton(onClick = { onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
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
                                elevation = 8.dp,
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
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // TODO Верим что появятся главы
            IconButton(onClick = { onSettingsClick() }) {
                Icon(Icons.Default.List, contentDescription = "Chapters")
            }

            Column {
                Slider(
                    value = firstVisibleItemIndex.toFloat(),
                    onValueChange = { newValue ->
                        CoroutineScope(Dispatchers.Main).launch {
                            lazyListState.scrollToItem(newValue.toInt())
                            onPageChange(newValue.toInt())
                        }
                    },
                    valueRange = 0f..(totalPagesState - 1).toFloat(), // Диапазон значений
                    steps = totalPagesState, // Количество шагов между min и max
                    modifier = Modifier.fillMaxWidth(0.8f)
                )

                Text(
                    text = "Page ${firstVisibleItemIndex + 1} of $totalPagesState",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { onSettingsClick() }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}
