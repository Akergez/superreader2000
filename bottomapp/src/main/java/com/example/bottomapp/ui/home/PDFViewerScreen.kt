package com.example.bottomapp.ui.home

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import com.github.barteksc.pdfviewer.PDFView
import com.example.bottomapp.R

@SuppressLint("UnrememberedMutableInteractionSource")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFViewerScreen(
    navigateUp: () -> Unit
) {

    Log.e(TAG, "PDFViewerScreen: ************** ")
    val context = LocalContext.current

    val inputStream = context.assets.open("gold.pdf")
    AndroidView(factory = { ctx ->
        val adView = PDFView(ctx, null)
        adView.fromStream(inputStream).enableDoubletap(true).spacing(10).load()
        adView
    }, modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 8.dp), update = { pdfViewer ->
        pdfViewer.doOnLayout {

        }
    })
}