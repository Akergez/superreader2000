package com.akergez.superreader2000.shared.formats.fb2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Base64
import androidx.core.graphics.createBitmap
import com.akergez.superreader2000.shared.backend.previewGenerators.FileWrapper
import com.akergez.superreader2000.shared.backend.previewGenerators.IPreviewGenerator
import com.akergez.superreader2000.shared.backend.previewGenerators.Image
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FileInputStream

actual class Fb2PreviewGenerator :
    IPreviewGenerator {
    override suspend fun generatePreview(filePath: FileWrapper): Image? {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(FileInputStream(filePath.file), "UTF-8")

            var title = ""
            var author = ""
            var coverData: String? = null
            var coverId: String? = null
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "book-title" -> title = parser.nextText()
                            "first-name", "last-name" -> author += parser.nextText() + " "
                            "coverpage" -> {
                                // Ищем ссылку на обложку
                                while (parser.next() != XmlPullParser.END_TAG) {
                                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "image") {
                                        coverId = parser.getAttributeValue(
                                            "http://www.w3.org/1999/xlink",
                                            "href"
                                        )?.removePrefix("#")
                                        break
                                    }
                                }
                            }

                            "binary" -> {
                                val id = parser.getAttributeValue(null, "id")
                                if (id == coverId && parser.getAttributeValue(null, "content-type")
                                        .startsWith("image/")
                                ) {
                                    coverData = parser.nextText()
                                    break
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            // Пытаемся создать битмап из данных обложки
            val coverBitmap = coverData?.let { decodeBase64ToBitmap(it) }

            // Если обложка найдена, используем ее
            if (coverBitmap != null) {
                return Image(coverBitmap)
            }

            // Если обложка не найдена, создаем стилизованное изображение с текстом
            val bitmap = createBitmap(300, 400)
            val canvas = Canvas(bitmap)

            // Заполняем фон градиентом
            val gradient = android.graphics.LinearGradient(
                0f, 0f, 300f, 400f,
                intArrayOf(Color.rgb(240, 240, 240), Color.rgb(220, 220, 220)),
                null, Shader.TileMode.CLAMP
            )
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

            return Image(bitmap)
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