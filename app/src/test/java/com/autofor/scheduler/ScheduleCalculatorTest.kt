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
}
