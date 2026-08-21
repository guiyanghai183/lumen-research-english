package com.lumen.researchenglish.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTranslationTest {
    @Test
    fun `translation card keeps quick translation before Tutor result`() {
        val markdown = readerTranslationMarkdown(
            quickTranslation = "这个结果说明……",
            tutorTranslation = "**自然译文**\n这项结果表明……",
            tutorProviderName = "Alibaba Qwen3.7 Flash",
        )

        assertTrue(markdown.startsWith("**快速直译 / Quick translation**"))
        assertTrue(markdown.contains("**Tutor 自然译解 · Alibaba Qwen3.7 Flash**"))
        assertTrue(markdown.indexOf("这个结果说明") < markdown.indexOf("这项结果表明"))
    }

    @Test
    fun `passage prompt requests natural translation instead of dictionary note`() {
        val prompt = readerTranslationPrompt(
            selection = "These findings should be interpreted with caution.",
            quickTranslation = "这些发现应谨慎解释。",
            nearbyContext = "The sample was small. These findings should be interpreted with caution.",
            singleWord = false,
        )

        assertTrue(prompt.contains("Translate the selected research-English passage"))
        assertTrue(prompt.contains("<selected_passage>"))
        assertTrue(prompt.contains("Tencent quick translation"))
        assertTrue(prompt.contains("Nearby context"))
        assertFalse(prompt.contains("dictionary note"))
    }

    @Test
    fun `single word prompt retains dictionary behavior`() {
        val prompt = readerTranslationPrompt(
            selection = "robust",
            quickTranslation = "稳健的",
            nearbyContext = "The model is robust to perturbations.",
            singleWord = true,
        )

        assertTrue(prompt.contains("dictionary note"))
        assertTrue(prompt.contains("<selected_word>"))
        assertFalse(prompt.contains("<selected_passage>"))
    }
}
