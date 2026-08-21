package com.lumen.researchenglish.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TutorApiProviderTest {
    @Test
    fun `saved provider can be restored`() {
        assertEquals(TutorApiProvider.DEEPSEEK, TutorApiProvider.fromStorage("deepseek"))
        assertEquals(TutorApiProvider.QWEN, TutorApiProvider.fromStorage("qwen"))
    }

    @Test
    fun `unknown provider safely keeps the existing DeepSeek default`() {
        assertEquals(TutorApiProvider.DEEPSEEK, TutorApiProvider.fromStorage(null))
        assertEquals(TutorApiProvider.DEEPSEEK, TutorApiProvider.fromStorage("unknown"))
    }
}
