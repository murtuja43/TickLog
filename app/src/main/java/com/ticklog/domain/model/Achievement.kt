package com.ticklog.domain.model

/**
 * The fixed catalogue of achievements a user can unlock.
 *
 * Deliberately small and non-gamified — milestones that mark genuine progress.
 * The presentation (label, icon) lives in the UI; the domain only models the
 * identity and the unlock rule's outcome.
 */
enum class AchievementType {
    /** A run of 7 consecutive perfect days. */
    STREAK_7,

    /** A run of 30 consecutive perfect days. */
    STREAK_30,

    /** 100 tasks completed in total. */
    TASKS_100,

    /** 365 tasks completed in total. */
    TASKS_365,

    /** A full calendar week in which every day was perfect. */
    PERFECT_WEEK,

    /** A full calendar month in which every day was perfect. */
    PERFECT_MONTH,
}

/**
 * An achievement together with whether the user has earned it.
 *
 * @property type which achievement.
 * @property unlocked whether it has been earned.
 */
data class Achievement(
    val type: AchievementType,
    val unlocked: Boolean,
)
