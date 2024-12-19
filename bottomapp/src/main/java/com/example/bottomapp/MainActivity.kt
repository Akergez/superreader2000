package com.example.bottomapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.bottomapp.backend.BookStorage
import com.example.bottomapp.backend.StorageManager
import com.example.bottomapp.ui.mainscreen.MainScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val storageManager = inject<StorageManager>()
    private val bookStorage = inject<BookStorage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        storageManager.value.clearFileCache()
        CoroutineScope(Dispatchers.IO).launch {
            bookStorage.value.saveBooksLinksToStorage()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen()
}