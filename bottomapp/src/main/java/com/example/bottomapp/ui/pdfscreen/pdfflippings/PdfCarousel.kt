package com.example.bottomapp.ui.pdfscreen.pdfflippings

import PdfRendererHelper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.bottomapp.backend.PdfFlipMode
import com.example.bottomapp.ui.pdfscreen.PdfPage

@Composable
fun PdfCarousel(
    totalPages: Int,
    initialPage: Int = 0,
    pdfFlipMode: PdfFlipMode,
    onPageChange: (Int) -> Unit,
    onToggleMode: (Mode) -> Unit,
    pdfRendererHelper: PdfRendererHelper,
    applyInversion: Boolean,
    onApplyInversionToggle: (Boolean) -> Unit
) {
    val lazyListState = rememberLazyListState(initialPage)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.weight(0.9f),
            state = lazyListState,
        ) {
            items(totalPages, key = { it }) { page ->
                Box(
                    Modifier
                        .fillParentMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Card(
                        Modifier
                            .fillParentMaxWidth(0.9f)
                            .align(Alignment.Center)
                    ) {
                        PdfPage(
                            page = page,
                            pdfRendererHelper = pdfRendererHelper,
                            pdfFlipMode = pdfFlipMode,
                            onToggleMode = onToggleMode,
                            applyInversion = applyInversion,
                            mode = Mode.Carousel
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

        // Page indicator and inversion toggle for the carousel
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page ${firstVisibleItemIndex + 1} of $totalPages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (applyInversion) "Inversion: On" else "Inversion: Off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable { onApplyInversionToggle(!applyInversion) }
            )
        }
    }
}
