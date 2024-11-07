import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.pdfscreen.pdfflippings.Mode
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.page.rememberPageCurlState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.koin.compose.koinInject

@Composable
fun Fb2ReaderScreen(book: Fb2BookDomainModel, settingsManager: SettingsManager = koinInject()) {
    var currentPage by remember { mutableIntStateOf(book.readProgress().pageIndex) }
    var mode by remember { mutableStateOf(Mode.Curl) }
    val context = LocalContext.current
    val fb2File = remember { book.getFile() }

    // Состояние для страниц
    var pages by remember { mutableStateOf(listOf<String>()) }

    // Настройки оформления
    val fontSize = 16; //settingsManager.getFontSize()
    val applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert

    // Парсинг FB2-контента
    LaunchedEffect(fb2File) {
        val fullText = parseFb2Content(fb2File)
        pages = withContext(Dispatchers.Default) {
            paginateText(fullText, fontSize, context)
        }
    }

    val onPageChange = rememberUpdatedState { newPage: Int ->
        currentPage = newPage
        book.saveProgress(Fb2Progress(newPage))
    }

    Fb2Content(
        currentPage = currentPage,
        totalPages = pages.size,
        pages = pages,
        mode = mode,
        onPageChange = onPageChange.value,
        onToggleMode = { newMode -> mode = newMode },
        settingsManager = settingsManager
    )
}

@OptIn(ExperimentalPageCurlApi::class)
@Composable
fun Fb2Content(
    currentPage: Int,
    totalPages: Int,
    pages: List<String>,
    mode: Mode,
    onPageChange: (Int) -> Unit,
    onToggleMode: (Mode) -> Unit,
    settingsManager: SettingsManager
) {
    val pagerState = rememberPageCurlState(initialCurrent = currentPage)

    LaunchedEffect(pagerState.current) {
        onPageChange(pagerState.current)
    }

    when (mode) {
        Mode.Carousel -> {
            Fb2Carousel(
                pages = pages,
                initialPage = currentPage,
                onPageChange = onPageChange,
                onToggleMode = onToggleMode,
                settingsManager = settingsManager
            )
        }
        Mode.Curl -> {
            PageCurl(
                count = totalPages,
                state = pagerState,
                config = rememberPageCurlConfig(
                    backPageContentAlpha = 0f,
                    backPageColor = MaterialTheme.colorScheme.background
                )
            ) { index ->
                Fb2Page(
                    pageContent = if (index < pages.size) pages[index] else "",
                    mode = Mode.Curl,
                    onToggleMode = onToggleMode,
                    settingsManager = settingsManager
                )
            }
        }
        Mode.Zoom -> {
            Fb2Page(
                pageContent = if (currentPage < pages.size) pages[currentPage] else "",
                mode = mode,
                onToggleMode = onToggleMode,
                settingsManager = settingsManager
            )
        }

        Mode.Scroll -> throw IllegalStateException("Scroll mode is not supported for FB2")
    }
}

@Composable
fun Fb2Carousel(
    pages: List<String>,
    initialPage: Int,
    onPageChange: (Int) -> Unit,
    onToggleMode: (Mode) -> Unit,
    settingsManager: SettingsManager
) {
    val lazyListState = rememberLazyListState(initialPage)
    val applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.weight(0.9f),
            state = lazyListState,
        ) {
            items(pages.size) { page ->
                Box(
                    Modifier
                        .fillParentMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Card(
                        Modifier
                            .fillParentMaxWidth(0.9f)
                            .align(Alignment.Center)
                    ) {
                        Fb2Page(
                            pageContent = pages[page],
                            mode = Mode.Carousel,
                            onToggleMode = onToggleMode,
                            settingsManager = settingsManager
                        )
                    }
                }
            }
        }

        val firstVisibleItemIndex by remember {
            derivedStateOf { lazyListState.firstVisibleItemIndex }
        }

        LaunchedEffect(firstVisibleItemIndex) {
            onPageChange(firstVisibleItemIndex)
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page ${firstVisibleItemIndex + 1} of ${pages.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (applyInversion) "Inversion: On" else "Inversion: Off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable {
                    settingsManager.setDocumentsTheme(
                        if (applyInversion) DocThemeMode.AsIs else DocThemeMode.Invert
                    )
                }
            )
        }
    }
}

@Composable
fun Fb2Page(
    pageContent: String,
    mode: Mode,
    onToggleMode: (Mode) -> Unit,
    settingsManager: SettingsManager
) {
    val haptic = LocalHapticFeedback.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isTransforming by remember { mutableStateOf(false) }

    val backgroundColor = if (settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert)
        Color.Black else Color.White
    val textColor = if (settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert)
        Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .let {
                if (mode == Mode.Zoom) {
                    it.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            isTransforming = true
                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                } else it
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isTransforming) {
                            onToggleMode(if (mode == Mode.Zoom) Mode.Curl else Mode.Zoom)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleMode(if (mode == Mode.Curl) Mode.Carousel else Mode.Curl )
                    }
                )
            }
    ) {
        Text(
            text = pageContent,
            fontSize = 18.sp * scale,
            color = textColor,
            modifier = Modifier.offset(offsetX.dp, offsetY.dp)
        )
    }
}

suspend fun parseFb2Content(fb2File: File): String {
    return withContext(Dispatchers.IO) {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(fb2File)

            // Нормализуем документ
            doc.documentElement.normalize()

            val stringBuilder = StringBuilder()

            // Найдем все элементы body
            val bodyElements = doc.getElementsByTagName("body")
            for (i in 0 until bodyElements.length) {
                val body = bodyElements.item(i)

                // Проверяем, является ли это основным телом текста
                val name = body.attributes?.getNamedItem("name")?.nodeValue
                if (name == null || name == "notes") {
                    parseBodyContent(body, stringBuilder)
                }
            }

            return@withContext stringBuilder.toString()
        } catch (e: Exception) {
            Log.e("FB2Parser", "Error parsing FB2: ${e.message}")
            e.printStackTrace()
            return@withContext "Ошибка при парсинге книги: ${e.message}"
        }
    }
}

private fun parseBodyContent(node: Node, stringBuilder: StringBuilder) {
    val children = node.childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        when (child.nodeName) {
            "section" -> parseBodyContent(child, stringBuilder)
            "title" -> {
                val title = extractTextContent(child)
                if (title.isNotEmpty()) {
                    stringBuilder.append(title.uppercase()).append("\n\n")
                }
            }
            "p" -> {
                val paragraph = extractTextContent(child)
                if (paragraph.isNotEmpty()) {
                    stringBuilder.append(paragraph).append("\n\n")
                }
            }
            "empty-line" -> stringBuilder.append("\n")
        }
    }
}

private fun extractTextContent(node: Node): String {
    val stringBuilder = StringBuilder()
    val children = node.childNodes
    for (i in 0 until children.length) {
        val child = children.item(i)
        when (child.nodeType) {
            Node.TEXT_NODE -> {
                val text = child.nodeValue?.trim() ?: ""
                if (text.isNotEmpty()) {
                    stringBuilder.append(text).append(" ")
                }
            }
            Node.ELEMENT_NODE -> {
                when (child.nodeName) {
                    "emphasis", "strong", "sup", "sub", "code", "a" -> {
                        val text = extractTextContent(child)
                        if (text.isNotEmpty()) {
                            stringBuilder.append(text).append(" ")
                        }
                    }
                }
            }
        }
    }
    return stringBuilder.toString().trim()
}

fun paginateText(fullText: String, fontSize: Int, context: Context): List<String> {
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    val paint = Paint()
    paint.textSize = fontSize * displayMetrics.density

    val textBounds = Rect()
    val pages = mutableListOf<String>()
    var currentPage = StringBuilder()
    var currentLineHeight = 0f
    var currentPageHeight = 0f

    fullText.split("\n").forEach { paragraph ->
        val words = paragraph.split(" ")
        var currentLine = StringBuilder()

        words.forEach { word ->
            val potentialLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            paint.getTextBounds(potentialLine, 0, potentialLine.length, textBounds)

            if (textBounds.width() > screenWidth) {
                // Line is full, add it to the current page
                currentPage.append(currentLine).append("\n")
                currentPageHeight += currentLineHeight
                currentLine = StringBuilder(word)
                currentLineHeight = paint.fontSpacing
            } else {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
                currentLineHeight = maxOf(currentLineHeight, paint.fontSpacing)
            }

            // Check if page is full
            if (currentPageHeight + currentLineHeight > screenHeight) {
                pages.add(currentPage.toString())
                currentPage = StringBuilder()
                currentPageHeight = 0f
            }
        }

        // Add the last line of the paragraph
        currentPage.append(currentLine).append("\n")
        currentPageHeight += currentLineHeight
        currentLineHeight = 0f

        // Add an extra line break between paragraphs
        currentPage.append("\n")
        currentPageHeight += paint.fontSpacing
    }

    // Add the last page if it's not empty
    if (currentPage.isNotEmpty()) {
        pages.add(currentPage.toString())
    }

    return pages
}
