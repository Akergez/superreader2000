package com.example.bottomapp.ui.readerscreens.fb2screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import com.example.bottomapp.R
import com.example.bottomapp.ui.readerscreens.Fb2BookContent

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
            text = getString(LocalContext.current, R.string.loading),
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