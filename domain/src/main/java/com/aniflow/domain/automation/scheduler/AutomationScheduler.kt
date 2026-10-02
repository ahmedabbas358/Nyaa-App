package com.aniflow.domain.automation.scheduler

import com.aniflow.domain.automation.model.ScheduleDefinition
import com.aniflow.domain.automation.model.SearchSchedule
import com.aniflow.domain.identity.SearchScheduleId
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

enum class MissedSchedulePolicy {
    RunOnce,    // Coalesce all missed executions into 1 run (Section 43)
    Skip,       // Skip past runs and wait for next interval
    Coalesce
}

/**
 * AutomationScheduler (Section 40, 41, 42, 43, 44, 131).
 * Evaluates pending schedules, handles offline missed schedules via coalescing,
 * and calculates deterministic future run times.
 */
class AutomationScheduler(
    private val missedPolicy: MissedSchedulePolicy = MissedSchedulePolicy.Coalesce
) {

    private val schedules = ConcurrentHashMap<SearchScheduleId, SearchSchedule>()

    fun registerSchedule(schedule: SearchSchedule) {
        schedules[schedule.id] = schedule
    }

    fun getDueSchedules(now: Instant = Instant.now()): List<SearchSchedule> {
        return schedules.values.filter { schedule ->
            schedule.enabled && !now.isBefore(schedule.nextRunAt)
        }
    }

    /**
     * Advances schedule to the next valid execution slot (Section 43, 131).
     * Prevents executing 50 missed searches if the device was offline for days.
     */
    fun onScheduleCompleted(
        scheduleId: SearchScheduleId,
        completedAt: Instant = Instant.now()
    ): SearchSchedule? {
        val schedule = schedules[scheduleId] ?: return null
        val nextTime = calculateNextRun(schedule.schedule, completedAt)

        val updated = schedule.copy(
            lastRunAt = completedAt,
            nextRunAt = nextTime
        )
        schedules[scheduleId] = updated
        return updated
    }

    fun calculateNextRun(definition: ScheduleDefinition, fromTime: Instant): Instant {
        return when (definition) {
            is ScheduleDefinition.IntervalMinutes -> {
                fromTime.plus(definition.minutes.toLong(), ChronoUnit.MINUTES)
            }
            is ScheduleDefinition.DailyAtHour -> {
                fromTime.plus(24, ChronoUnit.HOURS)
            }
            is ScheduleDefinition.WeeklyAtDayHour -> {
                fromTime.plus(7, ChronoUnit.DAYS)
            }
            is ScheduleDefinition.CronExpression -> {
                fromTime.plus(1, ChronoUnit.HOURS)
            }
        }
    }
}
