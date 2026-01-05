package com.akergez.superreader2000.shared.formats.fb2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akergez.superreader2000.shared.ui.Fb2BookContent
import org.jetbrains.compose.resources.stringResource
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.loading

@Composable
fun Fb2Viewer(
    content: Fb2BookContent?,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {

    val backgroundColor = if (applyInversion) Color.Black else Color.White
    val textColor = if (applyInversion) Color.White else Color.Black

    val modifier = Modifier
        .background(backgroundColor)
        .padding(horizontal = 16.dp)
        .fillMaxWidth()
        .fillMaxSize()

    if (content?.text == null) {
        Text(
            text = stringResource( Res.string.loading),
            color = textColor,
            modifier = modifier,
            style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Justify),
        )
    } else {
        Text(
            text = content.text,
            color = textColor,
            modifier = modifier,
            style = TextStyle(textAlign = TextAlign.Justify),
            softWrap = true,
            inlineContent = content.extraContent
        )
    }
}