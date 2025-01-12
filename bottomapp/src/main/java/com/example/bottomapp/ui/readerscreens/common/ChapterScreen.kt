package com.example.bottomapp.ui.readerscreens.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.bottomapp.backend.renderHelper.TOCEntry
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ChapterScreen(
    tableOfContents: StateFlow<List<TOCEntry>>,
    onChapterClick: (Int) -> Unit
) {
    val tableOfContentsValue by tableOfContents.collectAsState()
    LazyColumn {
        items(
            tableOfContentsValue.flatten(
                { it.children },
                { e, n -> ChapterView(e.title, n, e.pageIndex) })
        )
        { chapter ->
            Row(Modifier.pointerInput(Unit) {
                detectTapGestures { onChapterClick(chapter.page) }
            }.padding(start = chapter.nestingLevel * 16.dp))
            {
                Column {
                    Text(chapter.title)
                    Text(chapter.page.toString())
                }
            }
        }
    }
}

fun <TInitial, TResult> List<TInitial>.flatten(
    getChilds: (TInitial) -> List<TInitial>,
    constructFromNestingLevel: (TInitial, Int) -> TResult,
    nestingLevel: Int = 0
): List<TResult> {
    val result = mutableListOf<TResult>()
    forEach { entry ->
        result.add(constructFromNestingLevel(entry, nestingLevel))
        result.addAll(
            getChilds(entry).flatten(
                getChilds,
                constructFromNestingLevel,
                nestingLevel + 1
            )
        )
    }
    return result
}

data class ChapterView(
    val title: String,
    val nestingLevel: Int,
    val page: Int
)