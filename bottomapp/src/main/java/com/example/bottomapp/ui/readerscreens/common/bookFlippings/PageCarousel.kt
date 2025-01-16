package com.example.bottomapp.ui.readerscreens.common.bookFlippings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.BookPage
import com.example.bottomapp.ui.readerscreens.common.detectCustomTransformGestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageCarousel(
    bookContent: (Int) -> StateFlow<IBookContent>,
    totalPages: StateFlow<Int?>,
    onPageChange: (Int) -> Unit,
    onBack: () -> Unit,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    title: String,
    onSettingsClick: () -> Unit,
    onChaptersClick: () -> Unit,
    transformGestureHandler: (Offset, Offset, Float, Float, PointerInputChange, List<PointerInputChange>) -> Unit
) {
    val lazyListState = rememberLazyListState(initialPage)
    val totalPagesState by totalPages.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { padding ->
        totalPagesState?.let { totalPagesNotNull ->
            Column(modifier = modifier.padding(padding)) {
                LazyRow(
                    modifier = Modifier.weight(0.9f),
                    state = lazyListState,
                ) {
                    items(totalPagesNotNull, key = { it }) { page ->
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
                                    applyInversion = applyInversion,
                                    Modifier.pointerInput(Unit) {
                                        detectCustomTransformGestures(
                                            onGesture = transformGestureHandler,
                                            consume = false
                                        )
                                    },
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
                    IconButton(onClick = { onChaptersClick() }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chapters")
                    }

                    Column(
                    ) {
                        Slider(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .offset(y = 8.dp),
                            value = firstVisibleItemIndex.toFloat(),
                            onValueChange = { newValue ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    lazyListState.scrollToItem(newValue.toInt())
                                    onPageChange(newValue.toInt())
                                }
                            },
                            valueRange = 0f..(totalPagesNotNull - 1).toFloat(), // Диапазон значений
                            steps = totalPagesNotNull, // Количество шагов между min и max ,
                            track = {
                                Box(
                                    Modifier
                                        .height(2.dp) // Тонкий трек
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(50),
                                        ) // Цвет трека
                                )
                            },
                            thumb = {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .offset(y = 3.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }
                        )

                        Text(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
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
    }
}
