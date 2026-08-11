package com.lumen.researchenglish.domain

import java.time.LocalDate

data class DailyCheckInStats(
    val checkedInToday: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalDays: Int,
)

object DailyCheckIn {
    fun stats(checkInDates: Set<LocalDate>, today: LocalDate = LocalDate.now()): DailyCheckInStats {
        val dates = checkInDates.filterTo(sortedSetOf()) { !it.isAfter(today) }
        var currentStreak = 0
        var cursor = if (today in dates) today else today.minusDays(1)
        while (cursor in dates) {
            currentStreak += 1
            cursor = cursor.minusDays(1)
        }

        var longestStreak = 0
        var runningStreak = 0
        var previous: LocalDate? = null
        dates.forEach { date ->
            val previousDate = previous
            runningStreak = if (previousDate != null && date == previousDate.plusDays(1)) {
                runningStreak + 1
            } else {
                1
            }
            longestStreak = maxOf(longestStreak, runningStreak)
            previous = date
        }

        return DailyCheckInStats(
            checkedInToday = today in dates,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalDays = dates.size,
        )
    }
}
