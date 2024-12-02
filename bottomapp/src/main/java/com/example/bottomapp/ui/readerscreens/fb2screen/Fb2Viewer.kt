package com.example.bottomapp.ui.readerscreens.fb2screen

import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun Fb2Viewer(
    text: String?,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val backgroundColor = if (applyInversion) Color.Black else Color.White
    val textColor = if (applyInversion) Color.White else Color.Black
    text?.let {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier.background(backgroundColor),
            style = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Left),
        )
    }
}