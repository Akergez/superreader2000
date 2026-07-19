package com.akergez.superreader2000.shared.formats.fb2

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

const val TITLE_CONTENT_TAG = "FB2_TITLE"

abstract class FB2Element {
    protected val text: AnnotatedString

    constructor(text: String) {
        this.text = buildAnnotatedString { append(text) }
    }
    constructor(annotatedString: AnnotatedString) {
        this.text = annotatedString
    }
    constructor(annotatedString: FB2Element) {
        this.text = annotatedString.toStyledString()
    }

    /** Используется при построении TOC без создания временного AnnotatedString. */
    open fun contributesWordOffset(): Boolean = text.text.isNotBlank()

    abstract fun toStyledString(): AnnotatedString
}

class FB2Text : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    // Размер обычного текста задаёт RenderConfig/TextStyle. Отдельный SpanStyle
    // на каждом слове дублировал 18.sp и делал layout книги на порядок тяжелее.
    override fun toStyledString(): AnnotatedString = text
}

class FB2BoldText : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(text)
            }
        }
    }
}

class FB2TitleText : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString = buildAnnotatedString {
        pushStringAnnotation(TITLE_CONTENT_TAG, "title")
        withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
            withStyle(SpanStyle(fontSize = 1.3.em, fontWeight = FontWeight.SemiBold)) {
                append(text)
            }
        }
        pop()
    }
}

class FB2SubtitleText : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(
                ParagraphStyle(textAlign = TextAlign.Center)
            ) {
                withStyle(SpanStyle(fontSize = 1.1.em, fontWeight = FontWeight.Medium)) {
                    append(text)
                }
            }
        }
    }
}


class FB2ColoredText : FB2Element {
    private val color: Color

    constructor(text: String, color: Color) : super(text) {
        this.color = color
    }
    constructor(annotatedString: AnnotatedString, color: Color) : super(annotatedString) {
        this.color = color
    }
    constructor(element: FB2Element, color: Color) : super(element) {
        this.color = color
    }

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = color)) {
                append(text)
            }
        }
    }
}

class FB2ItalicText : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(text)
            }
        }
    }
}

class FB2UnderlineText : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(text)
            }
        }
    }
}

class FB2CodeText : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                append(text)
            }
        }
    }
}

class FB2LinkText : FB2Element {
    private val url: String

    constructor(text: String, url: String) : super(text) {
        this.url = url
    }
    constructor(annotatedString: AnnotatedString, url: String) : super(annotatedString) {
        this.url = url
    }
    constructor(element: FB2Element, url: String) : super(element) {
        this.url = url
    }

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                SpanStyle(
                    color = Color.Blue,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(text)
            }
            pop()
        }
    }
}

class FB2Paragraph : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            append("\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0")
            append(text)
            append('\n')
        }
    }
}

class FB2Image(var imageId: String, var description: String = "") :
    FB2Element(description) {
    override fun contributesWordOffset(): Boolean = true

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            append('\n')
            appendInlineContent(id = imageId, alternateText = "[image]")
            append('\n')
            if (text.isNotBlank()) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontSize = 16.sp)) {
                    append(text)
                }
                append('\n')
            }
        }
    }
}
