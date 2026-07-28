package com.autofor.data

import org.junit.Assert.*
import org.junit.Test

class ForwardingRuleTest {

    @Test
    fun testFormatTimeRange() {
        val rule = ForwardingRule(
            startHour = 8,
            startMinute = 30,
            endHour = 17,
            endMinute = 15
        )
        assertEquals("08:30 - 17:15", rule.formatTimeRange())
    }

    @Test
    fun testFormatDays_Weekdays() {
        val rule = ForwardingRule(
            daysOfWeek = setOf(2, 3, 4, 5, 6)
        )
        assertEquals("Weekdays", rule.formatDays())
    }

    @Test
    fun testFormatDays_EveryDay() {
        val rule = ForwardingRule(
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7)
        )
        assertEquals("Every day", rule.formatDays())
    }

    @Test
    fun testFormatDays_Weekends() {
        val rule = ForwardingRule(
            daysOfWeek = setOf(1, 7)
        )
        assertEquals("Weekends", rule.formatDays())
    }

    @Test
    fun testFormatDays_CustomDays() {
        val rule = ForwardingRule(
            daysOfWeek = setOf(2, 4) // Mon, Wed
        )
        assertEquals("Mon, Wed", rule.formatDays())
    }
}
