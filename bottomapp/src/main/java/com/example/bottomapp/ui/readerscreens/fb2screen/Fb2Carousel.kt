package com.example.bottomapp.ui.readerscreens.fb2screen

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
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.FlipMode


@Composable
fun Fb2Carousel(
    pages: List<String>,
    initialPage: Int,
    onPageChange: (Int) -> Unit,
    onToggleMode: (FlipMode) -> Unit,
    settingsManager: SettingsManager
) {
    val lazyListState = rememberLazyListState(initialPage)
    val applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.weight(0.9f),
            state = lazyListState,
        ) {
            items(pages.size) { page ->
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
                        Fb2Page(
                            pageContent = pages[page],
                            mode = FlipMode.Carousel,
                            onToggleMode = onToggleMode,
                            settingsManager = settingsManager
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
                text = "Page ${firstVisibleItemIndex + 1} of ${pages.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (applyInversion) "Inversion: On" else "Inversion: Off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable {
                    settingsManager.setDocumentsTheme(
                        if (applyInversion) DocThemeMode.AsIs else DocThemeMode.Invert
                    )
                }
            )
        }
    }
}