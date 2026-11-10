package com.akergez.superreader2000

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akergez.superreader2000.shared.backend.BookManager
import com.akergez.superreader2000.shared.backend.BookStorage
import com.akergez.superreader2000.shared.sync.AndroidServerConnectHost
import com.akergez.superreader2000.shared.sync.ServerConnectReturns
import com.akergez.superreader2000.shared.ui.screens.mainscreen.MainScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val storageManager = inject<BookStorage>()
    private val bookManager = inject<BookManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // После пересоздания активити тот же интент приходит снова — возврат уже принят.
        if (savedInstanceState == null) deliverServerConnectReturn(intent)
        setContent {
            MainScreen()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deliverServerConnectReturn(intent)
    }

    /** Возврат со страницы сервера Tresse: проверит и покажет экран синхронизации. */
    private fun deliverServerConnectReturn(intent: Intent?) {
        AndroidServerConnectHost.returnedUri(intent?.data)?.let(ServerConnectReturns::deliver)
    }

    override fun onDestroy() {
        super.onDestroy()
        storageManager.value.clearFileCache()
        CoroutineScope(Dispatchers.IO).launch {
            bookManager.value.saveBookLinks()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen()
}
