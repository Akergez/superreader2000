package com.example.bottomapp.ui.readerscreens.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.renderHelper.TOCEntry
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.bookFlippings.CasualCurl
import com.example.bottomapp.ui.readerscreens.common.bookFlippings.PageCarousel
import com.example.bottomapp.ui.readerscreens.common.bookFlippings.PageCurl
import com.example.bottomapp.ui.readerscreens.common.bookFlippings.VerticalBookScroll
import com.example.bottomapp.ui.readerscreens.common.components.SettingsModal
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.page.rememberPageCurlState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import kotlin.math.abs

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun BookContent(
    currentPage: Int,
    title: String,
    totalPages: StateFlow<Int?>,
    tableOfContent: StateFlow<List<TOCEntry>>,
    mode: ReaderMode,
    flipMode: FlipMode,
    onPageChange: (Int) -> Unit,
    onToggleMode: (ReaderMode) -> Unit,
    onCarouselBack: () -> Unit,
    modifier: Modifier = Modifier,
    getPage: (Int) -> StateFlow<IBookContent>,
    documentTheme: DocThemeMode,
    updateDocumentThemeMode: (DocThemeMode) -> Unit,
    updateFlipMode: (FlipMode) -> Unit
) {
    Log.d("BookContent", "mode = $mode, flipMode = $flipMode, currentPage = $currentPage")
    val pagerState = rememberPageCurlState(initialCurrent = currentPage)
    LaunchedEffect(pagerState.current) {
        onPageChange(pagerState.current)
    }

    var scale by remember { mutableFloatStateOf(1f) } // State to control zoom scale
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var persistentZoom by remember { mutableStateOf(TransformState()) }
    val haptic = LocalHapticFeedback.current

    val showSettingsModal = remember { mutableStateOf(false) }

    val actualMode = rememberUpdatedState(mode)
    val zoomState = rememberZoomState()
    val coroutineScope = rememberCoroutineScope()

    val onToggleModeWrapper = { newMode: ReaderMode ->
        onToggleMode(newMode)
        coroutineScope.launch { zoomState.reset() }
    }

    LaunchedEffect(zoomState.scale)
    {
        val newScale = zoomState.scale
        Log.d("BookContent", "New scale: $newScale")
        if (newScale <= 0.9f) {
            if (actualMode.value != ReaderMode.Carousel) {
                Log.d("BookContent", "Updating mode from ${actualMode.value} to Carousel")
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleModeWrapper(ReaderMode.Carousel)
            }
        }

        if(newScale > 1f) {
            if (actualMode.value != ReaderMode.Reading) {
                Log.d("BookContent", "Updating mode from ${actualMode.value} to Reading")
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleModeWrapper(ReaderMode.Reading)
            }
        }
    }

    Box()
    {
        when (mode) {
            ReaderMode.Carousel -> {
                PageCarousel(
                    totalPages = totalPages,
                    initialPage = currentPage,
                    onPageChange = onPageChange,
                    onBack = { onCarouselBack() },
                    applyInversion = documentTheme == DocThemeMode.Invert,
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    bookContent = getPage,
                    title = title,
                    onSettingsClick = { showSettingsModal.value = !showSettingsModal.value },
                    onChaptersClick = { onToggleModeWrapper(ReaderMode.Chapters) },
                    zoomState = zoomState
                )
                if (showSettingsModal.value) {
                    if (showSettingsModal.value) {
                        SettingsModal(
                            onApplyInversionToggle = {
                                updateDocumentThemeMode(if (it) DocThemeMode.Invert else DocThemeMode.AsIs)
                            },
                            applyInversion = documentTheme == DocThemeMode.Invert,
                            onVerticalScroll = {
                                updateFlipMode(it)
                            },
                            scrollMode = flipMode,
                            onStateChange = { showSettingsModal.value = it }
                        )
                    }
                }
            }

            ReaderMode.Reading -> {
                when (flipMode) {
                    FlipMode.PagingCurl -> PageCurl(
                        totalPages = totalPages,
                        onPageChange = onPageChange,
                        currentPage = currentPage,
                        getPage = getPage,
                        documentTheme = documentTheme,
                        modifier = modifier,
                        zoomState = zoomState
                    )

                    FlipMode.VerticalScroll -> VerticalBookScroll(
                        totalPages = totalPages,
                        initialPage = currentPage,
                        onPageChange = onPageChange,
                        applyInversion = documentTheme == DocThemeMode.Invert,
                        getPage = getPage,
                        zoomState = zoomState
                    )

                    FlipMode.Casual -> CasualCurl(
                        totalPages = totalPages,
                        initialPage = currentPage,
                        onPageChange = onPageChange,
                        applyInversion = documentTheme == DocThemeMode.Invert,
                        getPage = getPage,
                        persistentZoom = persistentZoom,
                        zoomState = zoomState
                    )
                }
            }

            ReaderMode.Chapters -> ChapterScreen(
                onChapterClick = {
                    onPageChange(it)
                    onToggleModeWrapper(ReaderMode.Reading)
                },
                tableOfContents = tableOfContent,
                onBack = { onToggleModeWrapper(ReaderMode.Carousel) }
            )
        }
    }
}

suspend fun PointerInputScope.detectCustomTransformGestures(
    panZoomLock: Boolean = false,
    consume: Boolean = true,
    pass: PointerEventPass = PointerEventPass.Main,
    onGestureStart: (PointerInputChange) -> Unit = {},
    onGesture: (
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        rotation: Float,
        mainPointer: PointerInputChange,
        changes: List<PointerInputChange>
    ) -> Unit,
    onGestureEnd: (PointerInputChange) -> Unit = {}
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        // Wait for at least one pointer to press down, and set first contact position
        val down: PointerInputChange = awaitFirstDown(
            requireUnconsumed = false,
            pass = pass
        )
        onGestureStart(down)

        var pointer = down
        // Main pointer is the one that is down initially
        var pointerId = down.id

        do {
            val event = awaitPointerEvent(pass = pass)

            // If any position change is consumed from another PointerInputChange
            // or pointer count requirement is not fulfilled
            val canceled =
                event.changes.any { it.isConsumed }

            if (!canceled) {

                // Get pointer that is down, if first pointer is up
                // get another and use it if other pointers are also down
                // event.changes.first() doesn't return same order
                val pointerInputChange =
                    event.changes.firstOrNull { it.id == pointerId }
                        ?: event.changes.first()

                // Next time will check same pointer with this id
                pointerId = pointerInputChange.id
                pointer = pointerInputChange

                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion =
                        abs(rotation * kotlin.math.PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(
                            centroid,
                            panChange,
                            zoomChange,
                            effectiveRotation,
                            pointer,
                            event.changes
                        )
                    }

                    if (consume) {
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
        onGestureEnd(pointer)
    }
}