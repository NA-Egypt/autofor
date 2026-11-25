package com.autofor.scheduler

import com.autofor.data.ForwardingRule
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ScheduleCalculatorTest {

    @Test
    fun testIsRuleCurrentlyActive_WithinRange_ReturnsTrue() {
        val rule = ForwardingRule(
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 0,
            daysOfWeek = setOf(2, 3, 4, 5, 6), // Mon-Fri
            isEnabled = true
        )

        // Monday at 10:30 AM
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }

        assertTrue(ScheduleCalculator.isRuleCurrentlyActive(rule, cal.timeInMillis))
    }

    @Test
    fun testIsRuleCurrentlyActive_OutsideRange_ReturnsFalse() {
        val rule = ForwardingRule(
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 0,
            daysOfWeek = setOf(2, 3, 4, 5, 6), // Mon-Fri
            isEnabled = true
        )

        // Monday at 6:00 PM (18:00)
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
        }

        assertFalse(ScheduleCalculator.isRuleCurrentlyActive(rule, cal.timeInMillis))
    }

    @Test
    fun testIsRuleCurrentlyActive_DisabledRule_ReturnsFalse() {
        val rule = ForwardingRule(
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 0,
            daysOfWeek = setOf(2, 3, 4, 5, 6),
            isEnabled = false
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }

        assertFalse(ScheduleCalculator.isRuleCurrentlyActive(rule, cal.timeInMillis))
    }

    @Test
    fun testGetCurrentlyActiveRules_FiltersCorrectly() {
        val rule1 = ForwardingRule(
            id = "1",
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 0,
            daysOfWeek = setOf(2), // Monday
            isEnabled = true
        )
        val rule2 = ForwardingRule(
            id = "2",
            startHour = 18,
            startMinute = 0,
            endHour = 22,
            endMinute = 0,
            daysOfWeek = setOf(2), // Monday
            isEnabled = true
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }

        val active = ScheduleCalculator.getCurrentlyActiveRules(listOf(rule1, rule2), cal.timeInMillis)
        assertEquals(1, active.size)
        assertEquals("1", active.first().id)
    }

    @Test
    fun testCalculateNextTriggerMillis_FutureToday() {
        val rule = ForwardingRule(
            startHour = 14,
            startMinute = 0,
            daysOfWeek = setOf(Calendar.MONDAY),
            isEnabled = true
        )

        val nowCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val triggerMillis = ScheduleCalculator.calculateNextTriggerMillis(rule, 14, 0, nowCal.timeInMillis)
        assertNotNull(triggerMillis)

        val resultCal = Calendar.getInstance().apply { timeInMillis = triggerMillis!! }
        assertEquals(14, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(Calendar.MONDAY, resultCal.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun testSaturdayRuleActiveAndTrigger() {
        val rule = ForwardingRule(
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 0,
            daysOfWeek = setOf(Calendar.SATURDAY),
            isEnabled = true
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 0)
        }

        assertTrue(ScheduleCalculator.isRuleCurrentlyActive(rule, cal.timeInMillis))
        assertEquals("Sat", rule.formatDays())
    }

    private fun at(dayOfWeek: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun describe(millis: Long?): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = millis!! }
        return Triple(cal.get(Calendar.DAY_OF_WEEK), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private val fridayNight = ForwardingRule(
        id = "night",
        startHour = 22, startMinute = 0,
        endHour = 6, endMinute = 0,
        daysOfWeek = setOf(Calendar.FRIDAY),
        isEnabled = true
    )

    @Test
    fun overnightRule_endsNextMorningEvenWhenThatDayIsNotSelected() {
        val next = ScheduleCalculator.calculateNextEndMillis(fridayNight, at(Calendar.FRIDAY, 23))
        assertEquals(Triple(Calendar.SATURDAY, 6, 0), describe(next))
        // Exactly one day after the Friday start, not the following week.
        assertEquals(8 * 60 * 60 * 1000L, next!! - at(Calendar.FRIDAY, 22))
    }

    @Test
    fun overnightRule_endFoundAfterMidnight() {
        val now = at(Calendar.FRIDAY, 23) + 2 * 60 * 60 * 1000L // Saturday 01:00
        val next = ScheduleCalculator.calculateNextEndMillis(fridayNight, now)
        assertEquals(Triple(Calendar.SATURDAY, 6, 0), describe(next))
    }

    @Test
    fun overnightRule_activeAfterMidnightOnlyWhenPreviousDaySelected() {
        val saturday1am = at(Calendar.FRIDAY, 23) + 2 * 60 * 60 * 1000L
        assertTrue(ScheduleCalculator.isRuleCurrentlyActive(fridayNight, saturday1am))
        // Friday 01:00 belongs to a Thursday window, which isn't selected.
        assertFalse(ScheduleCalculator.isRuleCurrentlyActive(fridayNight, at(Calendar.FRIDAY, 1)))
        assertTrue(ScheduleCalculator.isRuleCurrentlyActive(fridayNight, at(Calendar.FRIDAY, 22)))
    }

    @Test
    fun sameDayRule_endMatchesStartDay() {
        val rule = ForwardingRule(startHour = 9, endHour = 17, daysOfWeek = setOf(Calendar.MONDAY))
        val next = ScheduleCalculator.calculateNextEndMillis(rule, at(Calendar.MONDAY, 10))
        assertEquals(Triple(Calendar.MONDAY, 17, 0), describe(next))
    }

    @Test
    fun effectiveRule_overlappingWindowsPickMostRecentStart() {
        val allDay = ForwardingRule(id = "a", targetPhoneNumber = "111", startHour = 8, endHour = 20, daysOfWeek = setOf(Calendar.MONDAY))
        val lunch = ForwardingRule(id = "b", targetPhoneNumber = "222", startHour = 12, endHour = 14, daysOfWeek = setOf(Calendar.MONDAY))
        val rules = listOf(allDay, lunch)

        assertEquals("a", ScheduleCalculator.getEffectiveRule(rules, at(Calendar.MONDAY, 10))?.id)
        assertEquals("b", ScheduleCalculator.getEffectiveRule(rules, at(Calendar.MONDAY, 13))?.id)
        // When lunch ends, forwarding should fall back to the all-day rule, not switch off.
        assertEquals("a", ScheduleCalculator.getEffectiveRule(rules, at(Calendar.MONDAY, 14))?.id)
        assertNull(ScheduleCalculator.getEffectiveRule(rules, at(Calendar.MONDAY, 20)))
    }
}
