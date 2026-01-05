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
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

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

    abstract fun toStyledString(): AnnotatedString
}

class FB2Text : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 18.sp)) {
                append(text)
            }
        }
    }
}

class FB2BoldText : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                append(text)
            }
        }
    }
}

class FB2SubtitleText : FB2Element {
    constructor(text: String) : super(text)
    constructor(annotatedString: AnnotatedString) : super(annotatedString)
    constructor(element: FB2Element) : super(element)

    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(
                ParagraphStyle(textIndent = TextIndent(firstLine = 30.sp))
            ) {
                withStyle(SpanStyle(fontSize = 18.sp)) {
                    append(FB2BoldText(text).toStyledString())
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
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
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
                    textDecoration = TextDecoration.LineThrough
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
            withStyle(SpanStyle(fontSize = 18.sp)) {
                append('\n')
                append("\t\t\t\t") // 4 таба для отступа
                append(text)
            }
        }
    }
}

class FB2Image(var imageId: String, var description: String = "") :
    FB2Element(description) {
    override fun toStyledString(): AnnotatedString {
        return buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 300.sp)) {
                withStyle(
                    ParagraphStyle(
                        lineBreak = LineBreak.Heading,
                    )
                ) {
                    appendInlineContent(id = imageId, alternateText = description)
                }
            }
        }
    }
}