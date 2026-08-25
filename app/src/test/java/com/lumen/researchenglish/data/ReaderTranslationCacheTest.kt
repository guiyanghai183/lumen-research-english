package com.lumen.researchenglish.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReaderTranslationCacheTest {
    @Test
    fun cacheKeyIsStableForTrimmedSelection() {
        val first = ReaderTranslationCache.cacheKey("book", 2, "QWEN", " passage ")
        val second = ReaderTranslationCache.cacheKey("book", 2, "QWEN", "passage")

        assertEquals(first, second)
    }

    @Test
    fun cacheKeySeparatesPageAndProvider() {
        val base = ReaderTranslationCache.cacheKey("book", 2, "QWEN", "passage")

        assertNotEquals(base, ReaderTranslationCache.cacheKey("book", 3, "QWEN", "passage"))
        assertNotEquals(base, ReaderTranslationCache.cacheKey("book", 2, "DEEPSEEK", "passage"))
    }
}
