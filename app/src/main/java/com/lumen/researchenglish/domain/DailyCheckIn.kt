package com.lumen.researchenglish.domain

import java.time.LocalDate

data class DailyCheckInStats(
    val checkedInToday: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalDays: Int,
    val checkInDates: Set<LocalDate>,
    val studyMillisByDate: Map<LocalDate, Long> = emptyMap(),
    val dailyGoalMillis: Long = DAILY_STUDY_GOAL_MILLIS,
    val today: LocalDate = LocalDate.now(),
) {
    val todayStudyMillis: Long
        get() = studyMillisByDate[today] ?: 0L

    fun progressFor(date: LocalDate): Float =
        if (dailyGoalMillis <= 0L) 0f
        else ((studyMillisByDate[date] ?: 0L).toDouble() / dailyGoalMillis)
            .coerceIn(0.0, 1.0)
            .toFloat()
}

const val DAILY_STUDY_GOAL_MINUTES = 40L
const val DAILY_STUDY_GOAL_MILLIS = DAILY_STUDY_GOAL_MINUTES * 60_000L

object DailyCheckIn {
    fun stats(
        checkInDates: Set<LocalDate>,
        today: LocalDate = LocalDate.now(),
        studyMillisByDate: Map<LocalDate, Long> = emptyMap(),
        dailyGoalMillis: Long = DAILY_STUDY_GOAL_MILLIS,
    ): DailyCheckInStats {
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
            checkInDates = dates,
            studyMillisByDate = studyMillisByDate
                .filterKeys { !it.isAfter(today) }
                .mapValues { (_, value) -> value.coerceAtLeast(0L) },
            dailyGoalMillis = dailyGoalMillis,
            today = today,
        )
    }
}
