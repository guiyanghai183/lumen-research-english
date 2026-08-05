package com.lumen.researchenglish.domain

import kotlin.math.pow
import kotlin.math.roundToInt

data class LearningProgress(
    val totalXp: Int,
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
    val progress: Float,
    val evolutionStage: Int,
    val evolutionName: String,
)

object LearningLeveling {
    const val MAX_LEVEL = 100

    fun fromTotalXp(totalXp: Int): LearningProgress {
        val safeTotal = totalXp.coerceAtLeast(0)
        var remaining = safeTotal
        var level = 1
        while (level < MAX_LEVEL) {
            val requirement = xpRequired(level)
            if (remaining < requirement) break
            remaining -= requirement
            level += 1
        }
        val requirement = if (level == MAX_LEVEL) 0 else xpRequired(level)
        val stage = when {
            level >= 90 -> 3
            level >= 60 -> 2
            level >= 30 -> 1
            else -> 0
        }
        return LearningProgress(
            totalXp = safeTotal,
            level = level,
            xpIntoLevel = if (level == MAX_LEVEL) 0 else remaining,
            xpForNextLevel = requirement,
            progress = if (level == MAX_LEVEL) 1f else {
                remaining.toFloat() / requirement.toFloat().coerceAtLeast(1f)
            },
            evolutionStage = stage,
            evolutionName = listOf("Hatchling", "Explorer", "Scholar", "Guardian Scholar")[stage],
        )
    }

    /** Every later level deliberately needs more XP than the previous one. */
    fun xpRequired(level: Int): Int {
        val safeLevel = level.coerceIn(1, MAX_LEVEL - 1)
        return 60 + 12 * safeLevel + (1.8 * safeLevel.toDouble().pow(1.45)).roundToInt()
    }
}
