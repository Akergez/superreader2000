import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Locale

class PreviewGenerator(private val cacheSize: Int = 10) {

    private val bitmapCache: LruCache<String, Bitmap> = LruCache(cacheSize)

    fun getPreviewBitmap(file: File): Bitmap? {
        val cacheKey = file.absolutePath
        return bitmapCache[cacheKey] ?: generatePreviewBitmap(file)?.also {
            bitmapCache.put(cacheKey, it)
        }
    }

    private fun generatePreviewBitmap(file: File): Bitmap? {
        return when (file.extension.lowercase(Locale.getDefault())) {
            "pdf" -> generatePdfPreview(file)
            "fb2" -> generateFb2Preview(file)
            else -> null
        }
    }

    private fun generatePdfPreview(file: File): Bitmap? {
        var pdfRenderer: PdfRenderer? = null
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        try {
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                val width = page.width
                val height = page.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                return bitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun generateFb2Preview(file: File): Bitmap? {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(FileInputStream(file), "UTF-8")

            var title = ""
            var author = ""
            var coverData: String? = null
            var eventType = parser.eventType
            var insideCoverpage = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "book-title" -> title = parser.nextText()
                            "first-name", "last-name" -> author += parser.nextText() + " "
                            "coverpage" -> insideCoverpage = true
                            "binary" -> {
                                if (insideCoverpage && parser.getAttributeValue(null, "content-type").startsWith("image/")) {
                                    coverData = parser.nextText()
                                    break
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "coverpage") {
                            insideCoverpage = false
                        }
                    }
                }
                if (coverData != null) break
                eventType = parser.next()
            }

            // Пытаемся создать битмап из данных обложки
            val coverBitmap = coverData?.let { decodeBase64ToBitmap(it) }

            // Если обложка найдена, используем ее
            if (coverBitmap != null) {
                return coverBitmap
            }

            // Если обложка не найдена, создаем стилизованное изображение с текстом
            val bitmap = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Заполняем фон градиентом
            val gradient = LinearGradient(0f, 0f, 300f, 400f,
                intArrayOf(Color.rgb(240, 240, 240), Color.rgb(220, 220, 220)),
                null, Shader.TileMode.CLAMP)
            val paint = Paint()
            paint.shader = gradient
            canvas.drawRect(0f, 0f, 300f, 400f, paint)

            // Настраиваем кисть для текста заголовка
            val titlePaint = Paint().apply {
                color = Color.rgb(50, 50, 50)
                textSize = 30f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Настраиваем кисть для текста автора
            val authorPaint = Paint().apply {
                color = Color.rgb(100, 100, 100)
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }

            // Рисуем название (с переносом строк, если нужно)
            val titleLines = wrapText(title, 260f, titlePaint)
            titleLines.forEachIndexed { index, line ->
                canvas.drawText(line, 150f, 100f + index * 40f, titlePaint)
            }

            // Рисуем автора
            val authorLines = wrapText(author, 260f, titlePaint)
            authorLines.forEachIndexed { index, line ->
                canvas.drawText(line, 150f, 300f + index * 40f, authorPaint)
            }

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val result = mutableListOf<String>()
        val words = text.split(" ")
        var line = ""

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) <= maxWidth) {
                line = testLine
            } else {
                if (line.isNotEmpty()) result.add(line)
                line = word
            }
        }

        if (line.isNotEmpty()) result.add(line)
        return result
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}