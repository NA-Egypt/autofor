package com.autofor.scheduler

import com.autofor.data.ForwardingRule
import java.util.Calendar

object ScheduleCalculator {

    /**
     * Calculates the epoch milliseconds for the next occurrence of a target hour/minute
     * matching one of the enabled days of the week in [rule].
     */
    fun calculateNextTriggerMillis(
        rule: ForwardingRule,
        targetHour: Int,
        targetMinute: Int,
        fromMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (!rule.isEnabled || rule.daysOfWeek.isEmpty()) return null

        val calendar = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        for (dayOffset in 0..7) {
            val checkCal = (calendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
            }

            val checkDayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)
            if (rule.daysOfWeek.contains(checkDayOfWeek)) {
                if (checkCal.timeInMillis > fromMillis) {
                    return checkCal.timeInMillis
                }
            }
        }
        return null
    }

    /**
     * Checks if current timestamp falls within the scheduled time window for an active day in [rule].
     */
    fun isRuleCurrentlyActive(rule: ForwardingRule, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!rule.isEnabled) return false

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (!rule.daysOfWeek.contains(dayOfWeek)) return false

        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = rule.startHour * 60 + rule.startMinute
        val endMinutes = rule.endHour * 60 + rule.endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else { // Spans midnight
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    /**
     * Returns all enabled rules that are currently active at [nowMillis].
     */
    fun getCurrentlyActiveRules(rules: List<ForwardingRule>, nowMillis: Long = System.currentTimeMillis()): List<ForwardingRule> {
        return rules.filter { isRuleCurrentlyActive(it, nowMillis) }
    }
}

