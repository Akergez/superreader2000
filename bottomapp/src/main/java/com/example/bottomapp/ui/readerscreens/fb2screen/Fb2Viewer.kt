package com.example.bottomapp.ui.readerscreens.fb2screen

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bottomapp.backend.RenderHelper.IRenderHelper

@Composable
fun Fb2Viewer(
    renderHelper: IRenderHelper,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    applyInversion: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val text = renderHelper.openPagedText(pageIndex)

    Text(
        text = text,
        fontSize = 16.sp * scale,
        color = Color.Black,
        modifier = modifier.offset(offsetX.dp, offsetY.dp),
        textAlign = TextAlign.Left
    )
}