package com.example.bottomapp.shared.ui.screens.homescreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bottomapp.shared.ui.screens.readerscreens.common.FlipMode

@Composable
fun IconScrollSelector(onSelect: (FlipMode) -> Unit, selected: FlipMode) {
    val iconSize = 64.dp
    Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
        Column {
/*            Icon(
                painterResource(Res.drawable.),
                contentDescription = "Vertical Scroll",
                modifier = Modifier
                    .pointerInput(Unit) { detectTapGestures { onSelect(com.example.bottomapp.shared.ui.screens.readerscreens.common.FlipMode.VerticalScroll) } }
                    .align(Alignment.CenterHorizontally)
                    .conditional(selected == com.example.bottomapp.shared.ui.screens.readerscreens.common.FlipMode.VerticalScroll, {
                        border(
                            1.dp,
                            MaterialTheme.colorScheme.onBackground,
                            CircleShape
                        )
                    }).size(iconSize),
            )*/
            Spacer(modifier = Modifier.height(8.dp))
 /*           Text(
                getString(LocalContext.current, Res.string.),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )*/
        }


        Column {
/*            Icon(
                painterResource(Res.allDrawableResources.),
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
            )*/
            Spacer(modifier = Modifier.height(8.dp))
/*            Text(
                getString(LocalContext.current, R.string.paging_mode_casual),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )*/
        }


        Column {
/*            Icon(
                painterResource(R.drawable.classic_scroll_foreground),
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
                getString(LocalContext.current, R.string.paging_mode_classic),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )*/
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