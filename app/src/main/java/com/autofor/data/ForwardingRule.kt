package com.autofor.data

import java.util.UUID

data class ForwardingRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Forwarding Schedule",
    val targetPhoneNumber: String = "",
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0,
    val daysOfWeek: Set<Int> = setOf(2, 3, 4, 5, 6), // Mon(2) to Fri(6) in Java Calendar
    val isEnabled: Boolean = true
) {
    fun formatTimeRange(): String {
        val startFormatted = String.format("%02d:%02d", startHour, startMinute)
        val endFormatted = String.format("%02d:%02d", endHour, endMinute)
        return "$startFormatted - $endFormatted"
    }

    fun formatDays(): String {
        val dayNames = mapOf(
            1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed",
            5 to "Thu", 6 to "Fri", 7 to "Sat"
        )
        if (daysOfWeek.size == 7) return "Every day"
        if (daysOfWeek == setOf(2, 3, 4, 5, 6)) return "Weekdays"
        if (daysOfWeek == setOf(1, 7)) return "Weekends"
        return daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
    }
}
