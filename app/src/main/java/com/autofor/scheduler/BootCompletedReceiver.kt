package com.autofor.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduleManager = ScheduleManager(context)
            scheduleManager.rescheduleAll()
            scheduleManager.checkAndSyncActiveState()
        }

    }
}
