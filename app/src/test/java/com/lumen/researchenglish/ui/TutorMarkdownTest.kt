package com.lumen.researchenglish.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorMarkdownTest {
    @Test
    fun removesBoldMarkersAndStylesTheirContent() {
        val result = renderTutorMarkdown(
            markdown = "This is **important** context.",
            accentColor = Color.Blue,
            codeBackground = Color.LightGray,
        )

        assertEquals("This is important context.", result.text)
        assertTrue(
            result.spanStyles.any { range ->
                range.item.fontWeight == FontWeight.Bold &&
                    result.text.substring(range.start, range.end) == "important"
            },
        )
    }

    @Test
    fun rendersHeadingsBulletsAndInlineCode() {
        val result = renderTutorMarkdown(
            markdown = "## Meaning\n- keep `allowance` in context",
            accentColor = Color.Blue,
            codeBackground = Color.LightGray,
        )

        assertEquals("Meaning\n• keep allowance in context", result.text)
        assertTrue(result.spanStyles.any { it.item.fontFamily != null })
    }

    @Test
    fun hidesAnIncompleteBoldMarkerWhileStreaming() {
        val result = renderTutorMarkdown(
            markdown = "**Vivid translation is still streaming",
            accentColor = Color.Blue,
            codeBackground = Color.LightGray,
        )

        assertEquals("Vivid translation is still streaming", result.text)
    }
}
