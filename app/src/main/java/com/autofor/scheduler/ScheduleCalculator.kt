package com.autofor.scheduler

import com.autofor.data.ForwardingRule
import java.util.Calendar

object ScheduleCalculator {

    private const val MINUTES_PER_DAY = 24 * 60

    private val ForwardingRule.startMinutes get() = startHour * 60 + startMinute
    private val ForwardingRule.endMinutes get() = endHour * 60 + endMinute

    /** A window whose end time is earlier than its start time runs past midnight into the next day. */
    private val ForwardingRule.spansMidnight get() = endMinutes < startMinutes

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

    /** Next time a window of [rule] opens. */
    fun calculateNextStartMillis(rule: ForwardingRule, fromMillis: Long = System.currentTimeMillis()): Long? =
        calculateNextTriggerMillis(rule, rule.startHour, rule.startMinute, fromMillis)

    /**
     * Next time a window of [rule] closes. The selected days are the days a window *starts*,
     * so a Friday 22:00–06:00 window closes on Saturday 06:00 even when Saturday isn't selected.
     */
    fun calculateNextEndMillis(rule: ForwardingRule, fromMillis: Long = System.currentTimeMillis()): Long? {
        if (!rule.isEnabled || rule.daysOfWeek.isEmpty()) return null

        val calendar = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Start one day back so a window that opened yesterday and closes today is found.
        for (dayOffset in -1..7) {
            val startCal = (calendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, rule.startHour)
                set(Calendar.MINUTE, rule.startMinute)
            }
            if (!rule.daysOfWeek.contains(startCal.get(Calendar.DAY_OF_WEEK))) continue

            val endCal = (startCal.clone() as Calendar).apply {
                if (rule.spansMidnight) add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, rule.endHour)
                set(Calendar.MINUTE, rule.endMinute)
            }
            if (endCal.timeInMillis > fromMillis) return endCal.timeInMillis
        }
        return null
    }

    /**
     * Checks if current timestamp falls within the scheduled time window for an active day in [rule].
     * For windows that span midnight, the part after midnight belongs to the previous day.
     */
    fun isRuleCurrentlyActive(rule: ForwardingRule, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!rule.isEnabled) return false

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val today = cal.get(Calendar.DAY_OF_WEEK)
        val yesterday = if (today == Calendar.SUNDAY) Calendar.SATURDAY else today - 1

        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        return if (!rule.spansMidnight) {
            today in rule.daysOfWeek && currentMinutes in rule.startMinutes until rule.endMinutes
        } else {
            (today in rule.daysOfWeek && currentMinutes >= rule.startMinutes) ||
                (yesterday in rule.daysOfWeek && currentMinutes < rule.endMinutes)
        }
    }

    /**
     * Returns all enabled rules that are currently active at [nowMillis].
     */
    fun getCurrentlyActiveRules(rules: List<ForwardingRule>, nowMillis: Long = System.currentTimeMillis()): List<ForwardingRule> {
        return rules.filter { isRuleCurrentlyActive(it, nowMillis) }
    }

    /**
     * The rule whose number calls should be forwarded to at [nowMillis], or null when forwarding
     * should be off. When windows overlap, the one that started most recently wins.
     */
    fun getEffectiveRule(rules: List<ForwardingRule>, nowMillis: Long = System.currentTimeMillis()): ForwardingRule? {
        val active = getCurrentlyActiveRules(rules, nowMillis)
        if (active.isEmpty()) return null
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return active.minByOrNull { (currentMinutes - it.startMinutes + MINUTES_PER_DAY) % MINUTES_PER_DAY }
    }
}
