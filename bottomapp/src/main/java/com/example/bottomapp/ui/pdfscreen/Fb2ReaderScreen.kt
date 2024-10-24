package com.example.bottomapp.ui.home

import Mode
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.InputSource
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@Composable
fun Fb2ReaderScreen(book: Fb2BookDomainModel, settingsManager: SettingsManager) {
    var currentPage by remember { mutableIntStateOf(book.readProgress().pageIndex) }
    var mode by remember { mutableStateOf(Mode.Curl) }
    val context = LocalContext.current
    val fb2File = remember { book.getFile() }

    // Состояние для полного текста книги
    var fullText by remember { mutableStateOf("") }

    // Состояние для страниц
    var pages by remember { mutableStateOf(listOf<String>()) }

    // Настройки оформления из SettingsManager
    val fontSize =  10; //settingsManager.getFontSize() // Предполагается, что метод возвращает размер шрифта
    val backgroundColor = Color.Blue;//settingsManager.getBackgroundColor() // Предполагается, что метод возвращает цвет фона

    // Парсинг FB2-контента
    LaunchedEffect(fb2File) {
        fullText = parseFb2Content(fb2File)
        // После парсинга разделим текст на страницы
        pages = withContext(Dispatchers.Default) {
            paginateText(fullText, fontSize, context)
        }
    }

    // Обработка изменения страницы
    val onPageChange = { newPage: Int ->
        currentPage = newPage.coerceIn(0, pages.size - 1)
        // Сохранение прогресса чтения
        book.saveProgress(Fb2Progress(currentPage))
    }

    // Основной контент экрана
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = backgroundColor)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { /* Можно добавить логику для окончания жеста */ },
                    onHorizontalDrag = { change, dragAmount ->
                        if (dragAmount < 0) {
                            // Свайп влево: следующая страница
                            onPageChange(currentPage + 1)
                        } else {
                            // Свайп вправо: предыдущая страница
                            onPageChange(currentPage - 1)
                        }
                    }
                )
            }
    ) {
        if (pages.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = pages[currentPage]
                )
            }
        } else {
            // Отображение загрузки или сообщения об ошибке
            Text(
                text = "Загрузка...",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 18.sp,
                color = Color.Gray
            )
        }

        // Навигационные кнопки
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { onPageChange(currentPage - 1) },
                enabled = currentPage > 0
            ) {
                Text("Назад")
            }
            Text(
                text = "Страница ${currentPage + 1} из ${pages.size}",
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { onPageChange(currentPage + 1) },
                enabled = currentPage < pages.size - 1
            ) {
                Text("Вперед")
            }
        }
    }
}

/**
 * Парсит FB2-файл и извлекает текстовое содержимое.
 * Для простоты используем DOM-парсер. В реальном приложении рекомендуется использовать более эффективные методы.
 */
suspend fun parseFb2Content(fb2File: File): String {
    return withContext(Dispatchers.IO) {
        try {
            val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = builder.parse(InputSource(fb2File.inputStream().bufferedReader()))
            doc.documentElement.normalize()

            // Предполагается, что текст книги находится в тегах <body><p>...</p></body>
            val bodyNodes = doc.getElementsByTagName("p")
            val stringBuilder = StringBuilder()
            for (i in 0 until bodyNodes.length) {
                val node = bodyNodes.item(i)
                stringBuilder.append(node.textContent).append("\n\n")
            }
            stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Ошибка при парсинге книги."
        }
    }
}

/**
 * Разделяет полный текст на страницы based on font size and screen dimensions.
 * В реальном приложении необходимо учитывать размер экрана, размер шрифта и другие параметры.
 * Здесь приведен упрощённый пример.
 */
fun paginateText(fullText: String, fontSize: Int, context: android.content.Context): List<String> {
    val words = fullText.split("\\s+".toRegex())
    val linesPerPage = 30 // Примерное количество строк на странице
    val wordsPerPage = (linesPerPage * 10) // Примерное количество слов на страницу
    val pages = mutableListOf<String>()

    var currentPage = StringBuilder()
    var wordCount = 0

    for (word in words) {
        currentPage.append(word).append(" ")
        wordCount++
        if (wordCount >= wordsPerPage) {
            pages.add(currentPage.toString())
            currentPage = StringBuilder()
            wordCount = 0
        }
    }
    // Добавить оставшиеся слова
    if (currentPage.isNotEmpty()) {
        pages.add(currentPage.toString())
    }

    return pages
}