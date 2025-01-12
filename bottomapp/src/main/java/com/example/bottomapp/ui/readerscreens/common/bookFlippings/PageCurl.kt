package com.example.bottomapp.ui.readerscreens.common.bookFlippings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.ui.readerscreens.IBookContent
import com.example.bottomapp.ui.readerscreens.common.BookPage
import com.example.bottomapp.ui.readerscreens.common.basePageBoxModifier
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.rememberPageCurlState
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun PageCurl(
    totalPages: MutableStateFlow<Int>,
    currentPage: Int,
    getPage: (Int) -> MutableStateFlow<IBookContent>,
    documentTheme: DocThemeMode,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPageCurlState(initialCurrent = currentPage)
    val totalPagesState by totalPages.collectAsState()
    Scaffold { innerPadding ->
        eu.wewox.pagecurl.page.PageCurl(
            count = totalPagesState,
            state = pagerState,
            config = rememberPageCurlConfig(
                backPageContentAlpha = 0.5f,
                backPageColor = Color.Black
            ),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .systemBarsPadding()
        ) { index ->
            BookPage(
                bookContent = getPage(index),
                applyInversion = documentTheme == DocThemeMode.Invert,
                modifier = modifier
                    .basePageBoxModifier(),
            )
        }
    }
}