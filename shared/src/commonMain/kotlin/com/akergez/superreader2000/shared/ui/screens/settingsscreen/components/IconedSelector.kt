package com.akergez.superreader2000.shared.ui.screens.settingsscreen.components

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akergez.superreader2000.shared.ui.screens.readerscreens.common.FlipMode
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.classic_scroll_foreground
import superreader2000.shared.generated.resources.horizontal_scroll_foreground
import superreader2000.shared.generated.resources.paging_mode_casual
import superreader2000.shared.generated.resources.paging_mode_classic
import superreader2000.shared.generated.resources.vertical_scroll
import superreader2000.shared.generated.resources.vertical_scroll_foreground

@Composable
fun IconScrollSelector(onSelect: (FlipMode) -> Unit, selected: FlipMode) {
    val iconSize = 64.dp
    Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
        Column {
            Icon(
                painterResource(Res.drawable.vertical_scroll_foreground),
                contentDescription = "Vertical Scroll",
                modifier = Modifier
                    .pointerInput(Unit) { detectTapGestures { onSelect(FlipMode.VerticalScroll) } }
                    .align(Alignment.CenterHorizontally)
                    .conditional(selected == FlipMode.VerticalScroll, {
                        border(
                            1.dp,
                            MaterialTheme.colorScheme.onBackground,
                            CircleShape
                        )
                    }).size(iconSize),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(Res.string.vertical_scroll),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
        }


        Column {
            Icon(
                painterResource(Res.drawable.horizontal_scroll_foreground),
                contentDescription = "Horizontal Scroll",
                modifier = Modifier
                    .pointerInput(Unit) { detectTapGestures { onSelect(FlipMode.Casual) } }
                    .align(Alignment.CenterHorizontally)
                    .conditional(selected == FlipMode.Casual, {
                        border(
                            1.dp,
                            MaterialTheme.colorScheme.onBackground,
                            CircleShape
                        )
                    })
                    .size(iconSize),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(Res.string.paging_mode_casual),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
        }


        Column {
            Icon(
                painterResource(Res.drawable.classic_scroll_foreground),
                contentDescription = "Paging Curl",
                modifier = Modifier
                    .pointerInput(Unit) { detectTapGestures { onSelect(FlipMode.PagingCurl) } }
                    .align(Alignment.CenterHorizontally)
                    .conditional(selected == FlipMode.PagingCurl, {
                        border(
                            1.dp,
                            MaterialTheme.colorScheme.onBackground,
                            CircleShape
                        )
                    })
                    .size(iconSize),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(Res.string.paging_mode_classic),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Modifier.conditional(
    condition: Boolean,
    ifTrue: @Composable Modifier.() -> Modifier,
    ifFalse: (Modifier.() -> Modifier)? = null,
): Modifier {
    return if (condition) {
        then(ifTrue(Modifier))
    } else if (ifFalse != null) {
        then(ifFalse(Modifier))
    } else {
        this
    }
}