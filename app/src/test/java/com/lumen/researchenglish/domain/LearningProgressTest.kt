package com.lumen.researchenglish.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningProgressTest {
    @Test
    fun `pet evolves at levels 30 60 and 90`() {
        assertEquals(0, LearningLeveling.fromTotalXp(9_894).evolutionStage)
        assertEquals(1, LearningLeveling.fromTotalXp(9_895).evolutionStage)
        assertEquals(2, LearningLeveling.fromTotalXp(41_138).evolutionStage)
        assertEquals(3, LearningLeveling.fromTotalXp(97_873).evolutionStage)
    }

    @Test
    fun `later levels require more xp`() {
        assertEquals(74, LearningLeveling.xpRequired(1))
        assertTrue(LearningLeveling.xpRequired(60) > LearningLeveling.xpRequired(30))
        assertEquals(2_657, LearningLeveling.xpRequired(99))
    }
}
