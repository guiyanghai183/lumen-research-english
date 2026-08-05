package com.lumen.researchenglish.domain

import com.lumen.researchenglish.data.VocabularyCardEntity
import kotlin.math.exp

enum class MemoryStage(val label: String) {
    NEW("New"),
    LEARNING("Learning"),
    REMEMBERED("Memory"),
}

object MemoryModel {
    private const val DAY_MS = 86_400_000.0

    /** A transparent recall estimate based on the interval, stability, and review history. */
    fun strength(card: VocabularyCardEntity, now: Long = System.currentTimeMillis()): Float {
        if (card.repetitions == 0) return 0.04f
        val reviewedAt = card.lastReviewedAt.takeIf { it > 0 } ?: card.createdAt
        val elapsedDays = ((now - reviewedAt).coerceAtLeast(0L) / DAY_MS)
        val retention = exp(-elapsedDays / card.stability.coerceAtLeast(0.2))
        val evidence = 0.35 + 0.65 * (1.0 - exp(-card.repetitions / 2.5))
        val lapsePenalty = 1.0 / (1.0 + card.lapses * 0.12)
        return (retention * evidence * lapsePenalty).toFloat().coerceIn(0.04f, 1f)
    }

    /** The dashboard's memory rate: estimated recall across every saved card. */
    fun averageStrength(
        cards: Collection<VocabularyCardEntity>,
        now: Long = System.currentTimeMillis(),
    ): Float {
        if (cards.isEmpty()) return 0f
        return cards.sumOf { strength(it, now).toDouble() }
            .div(cards.size)
            .toFloat()
    }

    fun stage(card: VocabularyCardEntity, now: Long = System.currentTimeMillis()): MemoryStage = when {
        card.repetitions == 0 -> MemoryStage.NEW
        card.repetitions >= 3 && card.intervalDays >= 7 && strength(card, now) >= 0.7f -> {
            MemoryStage.REMEMBERED
        }
        else -> MemoryStage.LEARNING
    }
}
