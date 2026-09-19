package com.lumen.researchenglish.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCheckInTest {
    private val today = LocalDate.of(2026, 8, 11)

    @Test
    fun `counts a streak through today`() {
        val stats = DailyCheckIn.stats(
            setOf(today.minusDays(2), today.minusDays(1), today),
            today,
        )

        assertTrue(stats.checkedInToday)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        assertEquals(3, stats.totalDays)
        assertEquals(setOf(today.minusDays(2), today.minusDays(1), today), stats.checkInDates)
    }

    @Test
    fun `keeps yesterday streak available before today's check-in`() {
        val stats = DailyCheckIn.stats(
            setOf(today.minusDays(2), today.minusDays(1)),
            today,
        )

        assertFalse(stats.checkedInToday)
        assertEquals(2, stats.currentStreak)
    }

    @Test
    fun `separates current and longest streaks`() {
        val stats = DailyCheckIn.stats(
            setOf(today.minusDays(8), today.minusDays(7), today.minusDays(6), today.minusDays(1)),
            today,
        )

        assertEquals(1, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        assertEquals(4, stats.totalDays)
    }

    @Test
    fun `study progress follows the forty minute daily goal and is capped`() {
        val stats = DailyCheckIn.stats(
            checkInDates = setOf(today),
            today = today,
            studyMillisByDate = mapOf(
                today to 10L * 60_000L,
                today.minusDays(1) to 55L * 60_000L,
            ),
        )

        assertEquals(10L * 60_000L, stats.todayStudyMillis)
        assertEquals(0.25f, stats.progressFor(today), 0.0001f)
        assertEquals(1f, stats.progressFor(today.minusDays(1)), 0.0001f)
    }
}
