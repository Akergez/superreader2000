package com.akergez.superreader2000.shared.ui.screens.readerscreens.common.bookFlippings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalDensity
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
            // Detect orientation at the top level so we can size the lazy list correctly.
            BoxWithConstraints(
                modifier = modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                val isLandscape = constraints.maxWidth.toFloat() / constraints.maxHeight > 4f / 3f
                val pagesPerSpread = if (isLandscape) 2 else 1
                val spreadCount = (totalPagesNotNull + pagesPerSpread - 1) / pagesPerSpread

                var lastBookPage by remember { mutableIntStateOf(initialPage) }
                val lazyListState = rememberLazyListState(initialPage / pagesPerSpread)

                // When orientation changes scroll to the correct spread.
                LaunchedEffect(pagesPerSpread) {
                    lazyListState.scrollToItem(
                        (lastBookPage / pagesPerSpread).coerceIn(0, spreadCount - 1)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomable(zoomS)
                        .padding(top = 16.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.weight(0.9f),
                        state = lazyListState,
                    ) {
                        items(spreadCount, key = { it }) { spreadIndex ->
                            val leftPage = spreadIndex * pagesPerSpread
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillParentMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center,
                            ) {
                                val itemAvailW = constraints.maxWidth
                                val itemAvailH = constraints.maxHeight

                                if (isLandscape) {
                                    val rightPage = (leftPage + 1).takeIf { it < totalPagesNotNull }
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CarouselPageSlot(
                                                page = leftPage,
                                                availW = itemAvailW / 2,
                                                availH = itemAvailH,
                                                bookContent = bookContent,
                                                applyInversion = applyInversion,
                                                viewerProvider = viewerProvider,
                                            )
                                        }
                                        if (rightPage != null) {
                                            Box(
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                CarouselPageSlot(
                                                    page = rightPage,
                                                    availW = itemAvailW / 2,
                                                    availH = itemAvailH,
                                                    bookContent = bookContent,
                                                    applyInversion = applyInversion,
                                                    viewerProvider = viewerProvider,
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    CarouselPageSlot(
                                        page = leftPage,
                                        availW = itemAvailW,
                                        availH = itemAvailH,
                                        bookContent = bookContent,
                                        applyInversion = applyInversion,
                                        viewerProvider = viewerProvider,
                                    )
                                }
                            }
                        }
                    }

                    val firstVisibleSpread by remember {
                        derivedStateOf { lazyListState.firstVisibleItemIndex }
                    }
                    val sliderValue = remember { mutableFloatStateOf(initialPage.toFloat()) }

                    LaunchedEffect(firstVisibleSpread) {
                        val bookPage = firstVisibleSpread * pagesPerSpread
                        lastBookPage = bookPage
                        sliderValue.floatValue = bookPage.toFloat()
                        onPageChange(bookPage)
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

                        Column {
                            Slider(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .offset(y = 8.dp),
                                value = sliderValue.floatValue.coerceIn(0f, (totalPagesNotNull - 1).toFloat()),
                                onValueChange = { sliderValue.floatValue = it },
                                onValueChangeFinished = {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val targetSpread = (sliderValue.floatValue.toInt() / pagesPerSpread)
                                            .coerceIn(0, spreadCount - 1)
                                        lazyListState.scrollToItem(targetSpread)
                                        onPageChange(sliderValue.floatValue.toInt())
                                    }
                                },
                                valueRange = 0f..(totalPagesNotNull - 1).toFloat(),
                                steps = totalPagesNotNull,
                                track = {
                                    Box(
                                        Modifier
                                            .height(2.dp)
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(50),
                                            )
                                    )
                                },
                                thumb = {
                                    Box(
                                        Modifier
                                            .padding(4.dp)
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
                                text = "Page ${sliderValue.floatValue.toInt() + 1} of $totalPagesNotNull",
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
}

/**
 * Renders a single page inside the carousel using layout+graphicsLayer to scale
 * the native-size page content down to [availW]×[availH] without re-measuring text.
 */
@Composable
private fun <TBookContent : IBookContent> CarouselPageSlot(
    page: Int,
    availW: Int,
    availH: Int,
    bookContent: (Int) -> StateFlow<TBookContent>,
    applyInversion: Boolean,
    viewerProvider: IViewerProvider<TBookContent>,
) {
    val pageContent by bookContent(page).collectAsState()
    val nativeW = pageContent.nativeWidth
    val nativeH = pageContent.nativeHeight
    val density = LocalDensity.current

    if (nativeW > 0 && nativeH > 0) {
        val scale = minOf(availW.toFloat() / nativeW, availH.toFloat() / nativeH)
        val scaledWDp = with(density) { (nativeW * scale).toDp() }
        val scaledHDp = with(density) { (nativeH * scale).toDp() }

        // Fb2Viewer self-scales when canvas < native, so just give it the correct size.
        Box(
            modifier = Modifier
                .size(scaledWDp, scaledHDp)
                .shadow(8.dp, RoundedCornerShape(8.dp), clip = true)
        ) {
            BookPage(
                bookContent = bookContent(page),
                applyInversion = applyInversion,
                viewerProvider = viewerProvider,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}
