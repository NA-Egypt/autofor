package com.autofor.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.autofor.data.RuleRepository

class ForwardingExecutionService : Service() {

    companion object {
        const val CHANNEL_ID = "autofor_service_channel"
        const val CHANNEL_NAME = "AutoFor Execution Service"
        const val NOTIFICATION_ID = 2001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ScheduleManager.ACTION_TRIGGER_FORWARDING) {
            val enable = intent.getBooleanExtra(ScheduleManager.EXTRA_ENABLE_FORWARDING, false)
            val phoneNumber = intent.getStringExtra(ScheduleManager.EXTRA_PHONE_NUMBER) ?: ""

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val activityIntent = Intent(this, ForwardingActivity::class.java).apply {
                action = ScheduleManager.ACTION_TRIGGER_FORWARDING
                putExtra(ScheduleManager.EXTRA_ENABLE_FORWARDING, enable)
                putExtra(ScheduleManager.EXTRA_PHONE_NUMBER, phoneNumber)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pendingActivityIntent = PendingIntent.getActivity(
                this,
                (phoneNumber + enable).hashCode(),
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val mmiCode = if (enable) "*21*${phoneNumber.replace(Regex("[^0-9+]"), "")}#" else "#21#"
            val statusText = if (enable) "Executing call forwarding to $phoneNumber ($mmiCode)" else "Deactivating call forwarding ($mmiCode)"

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setContentTitle("AutoFor Automated Forwarding")
                .setContentText(statusText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingActivityIntent, true)
                .setContentIntent(pendingActivityIntent)
                .setAutoCancel(true)
                .build()

            startForeground(NOTIFICATION_ID, notification)

            // Trigger activity execution directly
            try {
                startActivity(activityIntent)
            } catch (e: Exception) {
                // Activity launch fallback handled by notification fullScreenIntent/contentIntent
            }

            // Reschedule future alarms
            ScheduleManager(this).rescheduleAll()

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }
}
