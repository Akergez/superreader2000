import android.content.Context
import android.util.Log
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bottomapp.backend.DocThemeMode
import com.example.bottomapp.backend.Fb2BookDomainModel
import com.example.bottomapp.backend.Fb2Progress
import com.example.bottomapp.backend.SettingsManager
import com.example.bottomapp.ui.readerscreens.common.FlipMode
import com.example.bottomapp.ui.readerscreens.fb2screen.Fb2Carousel
import com.example.bottomapp.ui.readerscreens.fb2screen.Fb2Page
import eu.wewox.pagecurl.ExperimentalPageCurlApi
import eu.wewox.pagecurl.config.rememberPageCurlConfig
import eu.wewox.pagecurl.page.PageCurl
import eu.wewox.pagecurl.page.rememberPageCurlState
import org.koin.compose.koinInject
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt


@Composable
fun Fb2ReaderScreen(book: Fb2BookDomainModel, settingsManager: SettingsManager = koinInject()) {
    var currentPage by remember { mutableIntStateOf(0) }
    var mode by remember { mutableStateOf(FlipMode.Curl) }
    val context = LocalContext.current
    val fb2File = remember { book.getFile() }

    // Состояние для страниц
    var pages by remember { mutableStateOf(listOf<String>()) }

    // Настройки оформления
    val fontSize = 16 //settingsManager.getFontSize()
    val applyInversion = settingsManager.getDocumentsTheme(DocThemeMode.AsIs) == DocThemeMode.Invert

    var sums by remember { mutableStateOf(listOf<Int>()) }

    val fullText = remember { parseFb2Content(fb2File) }

    val textMeasurer = rememberTextMeasurer()

    val navBarHeightPx = LocalDensity.current.run { 80.dp.toPx() }
    pages = remember { paginateText(fullText, fontSize, context, textMeasurer, navBarHeightPx.roundToInt()
    ) }

    sums = remember { calculatePrefixSums(pages) }
    val offset = book.readProgress().wordOffset;

    if (offset > 0) {
        for (i in 0..pages.size) {
            if (sums[i] > offset) {
                currentPage = i - 1
                break
            }
        }
    }

    val onPageChange = rememberUpdatedState { newPage: Int ->
        if (sums.isNotEmpty() && newPage > 0) {
            currentPage = newPage
            book.saveProgress(Fb2Progress(sums[newPage]))
        }
        if (newPage == 0) {
            currentPage = 0
        }
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
    mode: FlipMode,
    onPageChange: (Int) -> Unit,
    onToggleMode: (FlipMode) -> Unit,
    settingsManager: SettingsManager
) {
    val pagerState = rememberPageCurlState(initialCurrent = currentPage)

    LaunchedEffect(pagerState.current) {
        onPageChange(pagerState.current)
    }

    when (mode) {
        FlipMode.Carousel -> {
            Fb2Carousel(
                pages = pages,
                initialPage = currentPage,
                onPageChange = onPageChange,
                onToggleMode = onToggleMode,
                settingsManager = settingsManager
            )
        }

        FlipMode.Curl -> {
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
                    mode = FlipMode.Curl,
                    onToggleMode = onToggleMode,
                    settingsManager = settingsManager
                )
            }
        }

        FlipMode.Zoom -> {
            Fb2Page(
                pageContent = if (currentPage < pages.size) pages[currentPage] else "",
                mode = mode,
                onToggleMode = onToggleMode,
                settingsManager = settingsManager
            )
        }

        FlipMode.Scroll -> throw IllegalStateException("Scroll mode is not supported for FB2")
    }
}

fun parseFb2Content(fb2File: File): String {
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

        return stringBuilder.toString()
    } catch (e: Exception) {
        Log.e("FB2Parser", "Error parsing FB2: ${e.message}")
        e.printStackTrace()
        return "Ошибка при парсинге книги: ${e.message}"
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

            "empty-line" -> {
                stringBuilder.append("\n")
            }
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
                    // Вы можете добавить дополнительные теги, если нужно
                }
            }
        }
    }
    return stringBuilder.toString().trim()
}

fun paginateText(
    text: String,
    fontSize: Int,
    context: Context,
    textMeasurer: TextMeasurer,
    navBarHeightPx: Int
): List<String> {
    val displayMetrics = context.resources.displayMetrics
    val maxWidth = displayMetrics.widthPixels - (displayMetrics.density * 16).toInt()
    val maxHeight = displayMetrics.heightPixels - navBarHeightPx

    val pages = mutableListOf<String>()
    val words = text.split(" ", " ")
    var currentPage = StringBuilder()

    for (word in words) {
        val testLine = if (currentPage.isEmpty()) word else "$currentPage $word"

        val result = textMeasurer.measure(
            text = testLine,
            style = TextStyle(fontSize = fontSize.sp, textAlign = TextAlign.Center),
            constraints = Constraints(
                minWidth = maxWidth,
                maxWidth = maxWidth,
                minHeight = maxHeight,
                maxHeight = maxHeight
            ),
            softWrap = true,
        )

        if (!result.didOverflowHeight) {
            currentPage.append(if (currentPage.isEmpty()) word else " $word")
        } else {
            pages.add(currentPage.toString().trim())
            currentPage = StringBuilder()
            currentPage.append(word)
        }
    }

    if (currentPage.isNotEmpty()) {
        pages.add(currentPage.toString().trim())
    }

    return pages
}

fun calculatePrefixSums(pages: List<String>): List<Int> {
    val prefixSums = mutableListOf<Int>()
    var currentSum = 0

    for (page in pages) {
        prefixSums.add(currentSum)
        currentSum += page.length
    }

    return prefixSums
}