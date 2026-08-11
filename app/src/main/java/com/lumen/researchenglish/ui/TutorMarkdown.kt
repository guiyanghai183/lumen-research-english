package com.lumen.researchenglish.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Renders the compact Markdown subset used by Tutor replies without exposing raw markers such as
 * `**important**`. Keeping the parser local also makes streaming replies deterministic and avoids
 * introducing a WebView or a heavyweight HTML/Markdown dependency into chat bubbles.
 */
internal fun renderTutorMarkdown(
    markdown: String,
    accentColor: Color,
    codeBackground: Color,
): AnnotatedString = buildAnnotatedString {
    markdown.lines().forEachIndexed { index, rawLine ->
        val headingLevel = rawLine.takeWhile { it == '#' }.length
            .takeIf { it in 1..3 && rawLine.getOrNull(it) == ' ' }
        val lineWithoutHeading = if (headingLevel != null) {
            rawLine.drop(headingLevel + 1)
        } else {
            rawLine
        }
        val line = when {
            lineWithoutHeading.startsWith("- ") -> "• ${lineWithoutHeading.drop(2)}"
            lineWithoutHeading.startsWith("* ") -> "• ${lineWithoutHeading.drop(2)}"
            else -> lineWithoutHeading
        }
        val headingStyle = when (headingLevel) {
            1 -> SpanStyle(
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            2 -> SpanStyle(
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            3 -> SpanStyle(
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            else -> null
        }

        if (headingStyle == null) {
            appendInlineTutorMarkdown(line, accentColor, codeBackground)
        } else {
            withStyle(headingStyle) {
                appendInlineTutorMarkdown(line, accentColor, codeBackground)
            }
        }
        if (index != markdown.lines().lastIndex) append('\n')
    }
}

internal fun tutorMarkdownPlainText(markdown: String): String = renderTutorMarkdown(
    markdown = markdown,
    accentColor = Color.Unspecified,
    codeBackground = Color.Transparent,
).text

private fun AnnotatedString.Builder.appendInlineTutorMarkdown(
    line: String,
    accentColor: Color,
    codeBackground: Color,
) {
    var index = 0
    while (index < line.length) {
        val marker = when {
            line.startsWith("**", index) -> "**"
            line.startsWith("__", index) -> "__"
            line[index] == '`' -> "`"
            line[index] == '*' -> "*"
            line[index] == '_' -> "_"
            else -> null
        }
        if (marker == null) {
            append(line[index])
            index += 1
            continue
        }

        val closing = line.indexOf(marker, startIndex = index + marker.length)
        if (closing <= index + marker.length) {
            if (marker != "**" && marker != "__") append(marker)
            index += marker.length
            continue
        }

        val content = line.substring(index + marker.length, closing)
        val style = when (marker) {
            "**", "__" -> SpanStyle(
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
            "`" -> SpanStyle(
                background = codeBackground,
                fontFamily = FontFamily.Monospace,
            )
            else -> SpanStyle(fontStyle = FontStyle.Italic)
        }
        withStyle(style) { append(content) }
        index = closing + marker.length
    }
}
