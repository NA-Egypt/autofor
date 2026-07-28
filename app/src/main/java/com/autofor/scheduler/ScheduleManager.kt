package com.autofor.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.autofor.data.RuleRepository

class ScheduleManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val repository = RuleRepository(context)

    companion object {
        const val ACTION_TRIGGER_FORWARDING = "com.autofor.ACTION_TRIGGER_FORWARDING"
        const val EXTRA_ENABLE_FORWARDING = "extra_enable_forwarding"
        const val EXTRA_RULE_ID = "extra_rule_id"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }

    fun rescheduleAll() {
        cancelAllAlarms()

        if (!repository.isGlobalEnabled()) return

        val rules = repository.getRules()
        for (rule in rules) {
            if (!rule.isEnabled) continue
            scheduleRule(rule)
        }
    }

    /**
     * Checks if current time is within an active rule window and updates status if needed.
     */
    fun checkAndSyncActiveState() {
        if (!repository.isGlobalEnabled()) return

        val rules = repository.getRules()
        val activeRules = ScheduleCalculator.getCurrentlyActiveRules(rules)

        if (activeRules.isNotEmpty()) {
            val activeRule = activeRules.first()
            val mmiCode = "*21*${activeRule.targetPhoneNumber.replace(Regex("[^0-9+]"), "")}#"
            val statusText = "Active: Scheduled forwarding to ${activeRule.targetPhoneNumber} ($mmiCode)"
            repository.setLastForwardingStatus(statusText)
        }
    }


    @SuppressLint("ScheduleExactAlarm")
    fun scheduleRule(rule: com.autofor.data.ForwardingRule) {
        val nextStart = ScheduleCalculator.calculateNextTriggerMillis(rule, rule.startHour, rule.startMinute)
        val nextEnd = ScheduleCalculator.calculateNextTriggerMillis(rule, rule.endHour, rule.endMinute)

        if (nextStart != null) {
            val startIntent = Intent(context, ForwardingActivity::class.java).apply {
                action = ACTION_TRIGGER_FORWARDING
                putExtra(EXTRA_ENABLE_FORWARDING, true)
                putExtra(EXTRA_RULE_ID, rule.id)
                putExtra(EXTRA_PHONE_NUMBER, rule.targetPhoneNumber)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingStart = PendingIntent.getActivity(
                context,
                (rule.id + "_start").hashCode(),
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(nextStart, pendingStart)
        }

        if (nextEnd != null) {
            val endIntent = Intent(context, ForwardingActivity::class.java).apply {
                action = ACTION_TRIGGER_FORWARDING
                putExtra(EXTRA_ENABLE_FORWARDING, false)
                putExtra(EXTRA_RULE_ID, rule.id)
                putExtra(EXTRA_PHONE_NUMBER, rule.targetPhoneNumber)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingEnd = PendingIntent.getActivity(
                context,
                (rule.id + "_end").hashCode(),
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(nextEnd, pendingEnd)
        }
    }

    private fun setAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAllAlarms() {
        val rules = repository.getRules()
        for (rule in rules) {
            val startIntent = Intent(context, ForwardingActivity::class.java).apply {
                action = ACTION_TRIGGER_FORWARDING
            }
            val pendingStart = PendingIntent.getActivity(
                context,
                (rule.id + "_start").hashCode(),
                startIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingStart?.let { alarmManager.cancel(it) }

            val endIntent = Intent(context, ForwardingActivity::class.java).apply {
                action = ACTION_TRIGGER_FORWARDING
            }
            val pendingEnd = PendingIntent.getActivity(
                context,
                (rule.id + "_end").hashCode(),
                endIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingEnd?.let { alarmManager.cancel(it) }
        }
    }
}
