package com.example.bottomapp.shared.ui.screens.readerscreens.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.bottomapp.shared.renderHelper.TOCEntry
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(
    tableOfContents: StateFlow<List<TOCEntry>>,
    onChapterClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val tableOfContentsValue by tableOfContents.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
/*                    Text(
                        getString(LocalContext.current, R.string.table_of_contents),
                        style = MaterialTheme.typography.bodyLarge
                    )*/
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                       // Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { inner ->
        LazyColumn(Modifier.padding(inner)) {
            items(
                tableOfContentsValue.flatten(
                    { it.children },
                    { e, n -> ChapterView(e.title, n, e.pageIndex) })
            )
            { chapter ->
                ListItem(
                    headlineContent = {
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    supportingContent = {
/*                        Text(
                            getString(
                                LocalContext.current,
                                R.string.page
                            ) + ' ' + chapter.page.toString(), style = MaterialTheme.typography.bodySmall
                        )*/
                    },
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { onChapterClick(chapter.page) }
                        }
                        .padding(start = chapter.nestingLevel * 60.dp)
                )
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