package com.lumen.researchenglish.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VocabularyTranslationPreviewTest {
    @Test
    fun prefersNaturalTranslationInsteadOfMarkdownHeading() {
        val markdown = """
            **快速直译 / Quick translation**
            插图。

            **Tutor 自然译解 · Alibaba Qwen3.7 Flash**
            **自然译文 / Natural translation**
            图解；插图；用图像帮助说明的内容。

            **难点点拨 / Reading notes**
            常见于论文和教材。
        """.trimIndent()

        val preview = vocabularyTranslationPreview(markdown)

        assertEquals("图解；插图；用图像帮助说明的内容。", preview)
        assertFalse(preview.contains("快速直译"))
    }

    @Test
    fun fallsBackToQuickTranslationBody() {
        val markdown = """
            **快速直译 / Quick translation**
            区别；差别；卓越。
        """.trimIndent()

        assertEquals("区别；差别；卓越。", vocabularyTranslationPreview(markdown))
    }

    @Test
    fun phraseCardPrefersQuickTermDefinitionOverSurroundingSentenceTranslation() {
        val markdown = """
            **快速直译 / Quick translation**
            在……的情况下。

            **Tutor 自然译解 · Alibaba Qwen3.7 Flash**
            **自然译文 / Natural translation**
            对于那批数量不多且深受个人喜爱的作者而言，这一区分带来了一个有趣的后果。
        """.trimIndent()

        assertEquals(
            "在……的情况下。",
            vocabularyTranslationPreview(markdown, preferQuick = true),
        )
    }

    @Test
    fun phraseCardUsesContextualMeaningWhenQuickTranslationIsUnavailable() {
        val markdown = """
            Tutor 自然译解 · Alibaba Qwen3.7 Flash
            语境义 / Contextual meaning
            in the case of：在……的情况下；就……而言。

            常见义项 / Common senses
            用于限定所讨论的情形或对象。
        """.trimIndent()

        assertEquals(
            "in the case of：在……的情况下；就……而言。",
            vocabularyTranslationPreview(markdown, preferQuick = true),
        )
    }
}
