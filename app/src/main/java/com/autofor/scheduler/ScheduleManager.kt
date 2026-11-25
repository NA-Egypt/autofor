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
        const val EXTRA_IS_RETRY = "extra_is_retry"
        const val EXTRA_TRIGGER_AT = "extra_trigger_at"

        /** Pseudo rule ID for the one-off alarm that catches up on changes missed while the phone was off. */
        const val CATCH_UP_ID = "catch_up"
    }

    fun rescheduleAll() {
        val rules = repository.getRules()
        val globalEnabled = repository.isGlobalEnabled()

        // Include IDs of rules that have since been deleted, so their alarms don't keep firing.
        val knownIds = repository.getScheduledRuleIds() + rules.map { it.id }
        val liveIds = if (globalEnabled) rules.filter { it.isEnabled }.map { it.id }.toSet() else emptySet()

        for (id in knownIds) {
            cancelRuleAlarms(id)
            // Keep a pending retry only while its rule still exists and is enabled.
            if (id !in liveIds) cancelRetry(id)
        }
        if (!globalEnabled) cancelRetry(CATCH_UP_ID)

        repository.setScheduledRuleIds(liveIds)
        if (!globalEnabled) return

        for (rule in rules) {
            if (!rule.isEnabled) continue
            scheduleRule(rule)
        }
    }

    /**
     * The cleaned number calls should be forwarded to right now, or "" when forwarding should be off.
     */
    fun desiredForwardingNumber(nowMillis: Long = System.currentTimeMillis()): String {
        if (!repository.isGlobalEnabled()) return ""
        val rule = ScheduleCalculator.getEffectiveRule(repository.getRules(), nowMillis) ?: return ""
        return MmiCodes.cleanNumber(rule.targetPhoneNumber)
    }

    /**
     * After the phone was off, a window may have opened or closed without its alarm firing.
     * If what should be applied now differs from what AutoFor last dialed, schedule a one-off
     * trigger shortly after boot (giving the accessibility service time to connect).
     */
    fun scheduleCatchUpIfNeeded(delayMinutes: Long = 1): Boolean {
        if (!repository.isGlobalEnabled()) return false
        val desired = desiredForwardingNumber()
        val applied = repository.getAppliedForwardingNumber()
        // Never dialed and nothing should be forwarded: leave the carrier state alone.
        if (applied == null && desired.isEmpty()) return false
        if (applied == desired) return false
        scheduleRetry(desired.isNotEmpty(), CATCH_UP_ID, desired, delayMinutes, isRetry = false)
        return true
    }

    /**
     * Checks if current time is within an active rule window and updates status if needed.
     */
    fun checkAndSyncActiveState() {
        if (!repository.isGlobalEnabled()) return

        val activeRule = ScheduleCalculator.getEffectiveRule(repository.getRules())

        if (activeRule != null) {
            val mmiCode = MmiCodes.enableUnconditional(activeRule.targetPhoneNumber)
            val statusText = "Active: Scheduled forwarding to ${activeRule.targetPhoneNumber} ($mmiCode)"
            repository.setLastForwardingStatus(statusText)
        }
    }


    @SuppressLint("ScheduleExactAlarm")
    fun scheduleRule(rule: com.autofor.data.ForwardingRule) {
        val nextStart = ScheduleCalculator.calculateNextStartMillis(rule)
        val nextEnd = ScheduleCalculator.calculateNextEndMillis(rule)

        if (nextStart != null) {
            val startIntent = Intent(context, CallForwardingReceiver::class.java).apply {
                action = ACTION_TRIGGER_FORWARDING
                putExtra(EXTRA_ENABLE_FORWARDING, true)
                putExtra(EXTRA_RULE_ID, rule.id)
                putExtra(EXTRA_PHONE_NUMBER, rule.targetPhoneNumber)
                putExtra(EXTRA_TRIGGER_AT, nextStart)
            }
            val pendingStart = PendingIntent.getBroadcast(
                context,
                (rule.id + "_start").hashCode(),
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(nextStart, pendingStart)
        }

        if (nextEnd != null) {
            val endIntent = Intent(context, CallForwardingReceiver::class.java).apply {
                action = ACTION_TRIGGER_FORWARDING
                putExtra(EXTRA_ENABLE_FORWARDING, false)
                putExtra(EXTRA_RULE_ID, rule.id)
                putExtra(EXTRA_PHONE_NUMBER, rule.targetPhoneNumber)
                putExtra(EXTRA_TRIGGER_AT, nextEnd)
            }
            val pendingEnd = PendingIntent.getBroadcast(
                context,
                (rule.id + "_end").hashCode(),
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(nextEnd, pendingEnd)
        }
    }

    fun scheduleRetry(
        enable: Boolean,
        ruleId: String,
        phoneNumber: String,
        delayMinutes: Long = 2,
        isRetry: Boolean = true
    ) {
        val triggerAtMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        val retryIntent = Intent(context, CallForwardingReceiver::class.java).apply {
            action = ACTION_TRIGGER_FORWARDING
            putExtra(EXTRA_ENABLE_FORWARDING, enable)
            putExtra(EXTRA_RULE_ID, ruleId)
            putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(EXTRA_IS_RETRY, isRetry)
            putExtra(EXTRA_TRIGGER_AT, triggerAtMillis)
        }
        val pendingRetry = PendingIntent.getBroadcast(
            context,
            (ruleId + "_retry").hashCode(),
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(triggerAtMillis, pendingRetry)
    }

    private fun setAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            val showIntent = Intent(context, com.autofor.ui.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                1000,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
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
    }

    private fun cancelRuleAlarms(ruleId: String) {
        cancelAlarm(ruleId + "_start")
        cancelAlarm(ruleId + "_end")
    }

    fun cancelRetry(ruleId: String) {
        cancelAlarm(ruleId + "_retry")
    }

    private fun cancelAlarm(requestKey: String) {
        // PendingIntent matching ignores extras, so action + component + request code is enough.
        val intent = Intent(context, CallForwardingReceiver::class.java).apply {
            action = ACTION_TRIGGER_FORWARDING
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestKey.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
