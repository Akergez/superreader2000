package com.akergez.superreader2000.shared.ui.screens.readerscreens.common.bookFlippings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.akergez.superreader2000.shared.ui.IBookContent
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.BookPage
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.IViewerProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <TBookContent> PageCarousel(
    bookContent: (Int) -> StateFlow<TBookContent>,
    viewerProvider: IViewerProvider<TBookContent>,
    totalPages: StateFlow<Int?>,
    onPageChange: (Int) -> Unit,
    onBack: () -> Unit,
    applyInversion: Boolean,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    title: String,
    onSettingsClick: () -> Unit,
    onChaptersClick: () -> Unit,
    zoomState: (ZoomState) -> Unit
) where TBookContent : IBookContent {
    val lazyListState = rememberLazyListState(initialPage)
    val totalPagesState by totalPages.collectAsState()

    val zoomS = rememberZoomState()
    LaunchedEffect(zoomS.scale) { zoomState(zoomS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
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
            Column(
                modifier = modifier
                    .padding(padding)
                    .zoomable(zoomS)
                    .padding(top = 16.dp)
            ) {
                LazyRow(
                    modifier = Modifier.weight(0.9f),
                    state = lazyListState,
                ) {
                    items(totalPagesNotNull, key = { it }) { page ->
                        // Полный viewport — для корректного горизонтального скролла.
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .fillParentMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            val availW = constraints.maxWidth
                            val availH = constraints.maxHeight
                            val pageContent by bookContent(page).collectAsState()
                            val nativeW = pageContent.nativeWidth
                            val nativeH = pageContent.nativeHeight
                            if (nativeW > 0 && nativeH > 0) {
                                val scale = minOf(
                                    availW.toFloat() / nativeW,
                                    availH.toFloat() / nativeH,
                                )
                                val scaledWPx = (nativeW * scale).roundToInt()
                                val scaledHPx = (nativeH * scale).roundToInt()

                                // layout{} измеряет дочерний элемент в нативном размере,
                                // но сообщает родителю масштабированный размер.
                                // graphicsLayer визуально уменьшает результат как картинку.
                                Box(
                                    modifier = Modifier
                                        .layout { measurable, _ ->
                                            val placeable = measurable.measure(
                                                Constraints.fixed(nativeW, nativeH)
                                            )
                                            layout(scaledWPx, scaledHPx) {
                                                placeable.place(0, 0)
                                            }
                                        }
                                        .graphicsLayer {
                                            scaleX          = scale
                                            scaleY          = scale
                                            transformOrigin = TransformOrigin(0f, 0f)
                                        }
                                        .shadow(8.dp, RoundedCornerShape(8.dp), clip = true)
                                ) {
                                    BookPage(
                                        bookContent    = bookContent(page),
                                        applyInversion = applyInversion,
                                        viewerProvider = viewerProvider,
                                    )
                                }
                            } else {
                                // Заглушка пока нативные размеры ещё не известны.
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.9f)
                                        .shadow(8.dp, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                )
                            }
                        }
                    }
                }

                val firstVisibleItemIndex by remember {
                    derivedStateOf { lazyListState.firstVisibleItemIndex }
                }

                val sliderValue = remember { mutableFloatStateOf(firstVisibleItemIndex.toFloat()) }

                LaunchedEffect(firstVisibleItemIndex) {
                    sliderValue.value = firstVisibleItemIndex.toFloat()
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
                            value = sliderValue.value, // Используем локальное состояние
                            onValueChange = { newValue ->
                                sliderValue.value = newValue // Обновляем временное значение
                            },
                            onValueChangeFinished = {
                                // Вызываем событие только после завершения взаимодействия
                                CoroutineScope(Dispatchers.Main).launch {
                                    lazyListState.scrollToItem(sliderValue.value.toInt())
                                    onPageChange(sliderValue.value.toInt())
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
                            text = "Page ${sliderValue.value.toInt() + 1} of $totalPagesState",
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
