package com.lumen.researchenglish.domain

import com.lumen.researchenglish.data.VocabularyCardEntity
import kotlin.math.max
import kotlin.math.roundToInt

enum class ReviewRating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4),
}

object ReviewScheduler {
    private const val DAY_MS = 86_400_000L
    private const val MINUTE_MS = 60_000L

    fun schedule(
        card: VocabularyCardEntity,
        rating: ReviewRating,
        now: Long = System.currentTimeMillis(),
    ): VocabularyCardEntity {
        val newDifficulty = when (rating) {
            ReviewRating.AGAIN -> (card.difficulty + 0.9).coerceAtMost(10.0)
            ReviewRating.HARD -> (card.difficulty + 0.25).coerceAtMost(10.0)
            ReviewRating.GOOD -> (card.difficulty - 0.15).coerceAtLeast(1.0)
            ReviewRating.EASY -> (card.difficulty - 0.45).coerceAtLeast(1.0)
        }
        val newStability = when (rating) {
            ReviewRating.AGAIN -> max(0.2, card.stability * 0.55)
            ReviewRating.HARD -> max(0.8, card.stability * 1.35)
            ReviewRating.GOOD -> max(2.0, card.stability * (2.15 - newDifficulty * 0.055))
            ReviewRating.EASY -> max(4.0, card.stability * (3.0 - newDifficulty * 0.07))
        }
        val intervalDays = when {
            rating == ReviewRating.AGAIN && card.repetitions == 0 -> 0
            rating == ReviewRating.AGAIN -> 1
            rating == ReviewRating.HARD && card.repetitions == 0 -> 1
            rating == ReviewRating.GOOD && card.repetitions == 0 -> 3
            rating == ReviewRating.EASY && card.repetitions == 0 -> 7
            else -> newStability.roundToInt().coerceAtLeast(1)
        }
        val nextDue = if (rating == ReviewRating.AGAIN && card.repetitions == 0) {
            now + 10 * MINUTE_MS
        } else {
            now + intervalDays * DAY_MS
        }
        return card.copy(
            dueAt = nextDue,
            stability = newStability,
            difficulty = newDifficulty,
            intervalDays = intervalDays,
            repetitions = card.repetitions + 1,
            lapses = card.lapses + if (rating == ReviewRating.AGAIN) 1 else 0,
            lastReviewedAt = now,
        )
    }
}
