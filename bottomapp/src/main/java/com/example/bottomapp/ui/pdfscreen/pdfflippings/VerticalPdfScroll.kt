package com.example.bottomapp.ui.pdfscreen.pdfflippings

import PdfRendererHelper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bottomapp.ui.pdfscreen.PdfPage

@Composable
fun VerticalPdfScroll(
    totalPages: Int,
    initialPage: Int = 0,
    onPageChange: (Int) -> Unit,
    onToggleMode: (Mode) -> Unit,
    pdfRendererHelper: PdfRendererHelper,
    applyInversion: Boolean,
    onApplyInversionToggle: (Boolean) -> Unit
) {
    val lazyListState = rememberLazyListState(initialPage)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
        ) {
            items(totalPages, key = { it }) { page ->
                Box(
                    Modifier
                        .padding(vertical = 5.dp)
                ) {
                    Card(
                        Modifier
                            .fillParentMaxHeight()
                    ) {
                        PdfPage(
                            page = page,
                            pdfRendererHelper = pdfRendererHelper,
                            onPageChange = onPageChange,
                            onToggleMode = onToggleMode,
                            mode = Mode.Scroll,
                            applyInversion = applyInversion
                        )
                    }
                }
            }
        }

        // Emit page change when the page is scrolled
        val firstVisibleItemIndex by remember {
            derivedStateOf { lazyListState.firstVisibleItemIndex }
        }

        // Synchronize the page change
        LaunchedEffect(firstVisibleItemIndex) {
            onPageChange(firstVisibleItemIndex)
        }

    }
}