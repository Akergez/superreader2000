package com.example.bottomapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.bottomapp.backend.BookManager
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.ui.mainscreen.MainScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val storageManager = inject<BookStorage>()
    private val bookManager = inject<BookManager>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            enableEdgeToEdge()
            MainScreen()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        storageManager.value.clearFileCache()
        CoroutineScope(Dispatchers.IO).launch {
            bookManager.value.saveBookLinks()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen()
}