package com.autofor.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.autofor.R
import com.autofor.data.RuleRepository
import com.autofor.ui.MainActivity
import com.autofor.util.DeviceHealthChecker

class CallForwardingReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID_URGENT = "autofor_urgent_channel"
        const val CHANNEL_NAME_URGENT = "AutoFor Scheduled Triggers"

        const val CHANNEL_ID_ERROR = "autofor_error_channel"
        const val CHANNEL_NAME_ERROR = "AutoFor Troubleshooting & Alerts"

        const val CHANNEL_ID_INFO = "autofor_info_channel"
        const val CHANNEL_NAME_INFO = "AutoFor Status Updates"

        const val NOTIFICATION_ID_URGENT = 1001
        const val NOTIFICATION_ID_ERROR = 1002
        const val NOTIFICATION_ID_INFO = 1003
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ScheduleManager.ACTION_TRIGGER_FORWARDING) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AutoFor:TriggerWakeLock"
        )
        wakeLock?.acquire(15000L) // Keep CPU awake for up to 15s

        try {
            val enable = intent.getBooleanExtra(ScheduleManager.EXTRA_ENABLE_FORWARDING, false)
            val phoneNumber = intent.getStringExtra(ScheduleManager.EXTRA_PHONE_NUMBER) ?: ""
            val repository = RuleRepository(context)

            // Step 1: Validate CALL_PHONE permission
            if (!DeviceHealthChecker.hasCallPhonePermission(context)) {
                val errorMsg = "Missing CALL_PHONE permission. Scheduled forwarding could not be dialed."
                repository.setLastForwardingStatus("Failed: Missing CALL_PHONE permission")
                repository.setLastForwardingError(errorMsg)
                repository.setLastExecutionTime(System.currentTimeMillis())

                sendTroubleshootingNotification(
                    context,
                    title = "AutoFor: Action Required",
                    message = errorMsg,
                    resolutionStep = "Tap here to grant Phone Call permission in Android Settings so AutoFor can dial forwarding codes."
                )
                return
            }

            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val mmiCode = if (enable) "*21*$cleanNumber#" else "#21#"
            val actionLabel = if (enable) "Forwarding to $phoneNumber" else "Forwarding Cancellation"

            // Step 2: Attempt execution
            executeTrigger(context, enable, phoneNumber, mmiCode, actionLabel, repository)

        } finally {
            // Always reschedule next occurrences
            ScheduleManager(context).rescheduleAll()

            if (wakeLock != null && wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun executeTrigger(
        context: Context,
        enable: Boolean,
        phoneNumber: String,
        mmiCode: String,
        actionLabel: String,
        repository: RuleRepository
    ) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        var directSucceeded = false

        // Attempt 1: Direct activity launch if screen is on or overlay permission is granted
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenOn = powerManager?.isInteractive ?: false
        val hasOverlayPermission = Settings.canDrawOverlays(context)

        if (isScreenOn || hasOverlayPermission) {
            try {
                val activityIntent = Intent(context, ForwardingActivity::class.java).apply {
                    action = ScheduleManager.ACTION_TRIGGER_FORWARDING
                    putExtra(ScheduleManager.EXTRA_ENABLE_FORWARDING, enable)
                    putExtra(ScheduleManager.EXTRA_PHONE_NUMBER, phoneNumber)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(activityIntent)
                directSucceeded = true
                repository.setLastForwardingStatus("Triggered $actionLabel ($mmiCode)")
                repository.setLastForwardingError(null)
                repository.setLastExecutionTime(System.currentTimeMillis())
                return
            } catch (e: Exception) {
                directSucceeded = false
            }
        }

        // Attempt 2: Try silent TelephonyManager.sendUssdRequest if supported
        if (!directSucceeded && telephonyManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                telephonyManager.sendUssdRequest(
                    mmiCode,
                    object : TelephonyManager.UssdResponseCallback() {
                        override fun onReceiveUssdResponse(
                            telephonyManager: TelephonyManager?,
                            request: String?,
                            response: CharSequence?
                        ) {
                            val msg = "Auto-forwarded: $actionLabel ($mmiCode). Carrier response: $response"
                            repository.setLastForwardingStatus(msg)
                            repository.setLastForwardingError(null)
                            repository.setLastExecutionTime(System.currentTimeMillis())
                            sendInfoNotification(context, "AutoFor Forwarding Completed", msg)
                        }

                        override fun onReceiveUssdResponseFailed(
                            telephonyManager: TelephonyManager?,
                            request: String?,
                            failureCode: Int
                        ) {
                            // Carrier or device rejected silent USSD request; fallback to full-screen alert
                            postUrgentPromptNotification(context, enable, phoneNumber, mmiCode, actionLabel, repository)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
                return
            } catch (e: Exception) {
                // If sendUssdRequest fails or throws SecurityException, fallback to full-screen prompt
            }
        }

        // Attempt 3: Fail-safe Full-Screen Heads-Up Alarm Notification
        postUrgentPromptNotification(context, enable, phoneNumber, mmiCode, actionLabel, repository)
    }

    private fun postUrgentPromptNotification(
        context: Context,
        enable: Boolean,
        phoneNumber: String,
        mmiCode: String,
        actionLabel: String,
        repository: RuleRepository
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create urgent alarm channel with sound & vibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID_URGENT,
                CHANNEL_NAME_URGENT,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alarms and triggers for scheduled call forwarding"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(alarmSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val dialIntent = Intent(context, ForwardingActivity::class.java).apply {
            action = ScheduleManager.ACTION_TRIGGER_FORWARDING
            putExtra(ScheduleManager.EXTRA_ENABLE_FORWARDING, enable)
            putExtra(ScheduleManager.EXTRA_PHONE_NUMBER, phoneNumber)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingDial = PendingIntent.getActivity(
            context,
            (phoneNumber + enable).hashCode(),
            dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            (phoneNumber + enable + "_fs").hashCode(),
            dialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_URGENT)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("AutoFor: Scheduled $actionLabel")
            .setContentText("Tap to dial $mmiCode now")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Scheduled trigger ready: $actionLabel ($mmiCode).\nTap 'Dial Now' to immediately apply carrier call forwarding."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(pendingDial)
            .addAction(android.R.drawable.ic_menu_call, "Dial $mmiCode Now", pendingDial)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_URGENT, notification)

        val statusText = "Ready to dial: $actionLabel ($mmiCode). Tap notification to complete."
        repository.setLastForwardingStatus(statusText)
        repository.setLastExecutionTime(System.currentTimeMillis())
    }

    private fun sendTroubleshootingNotification(
        context: Context,
        title: String,
        message: String,
        resolutionStep: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_ERROR,
                CHANNEL_NAME_ERROR,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SHOW_TROUBLESHOOTING, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingApp = PendingIntent.getActivity(
            context,
            1099,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ERROR)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$message\n\nHow to fix:\n$resolutionStep"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingApp)
            .addAction(android.R.drawable.ic_menu_preferences, "Open Diagnostics", pendingApp)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ERROR, notification)
    }

    private fun sendInfoNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_INFO,
                CHANNEL_NAME_INFO,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INFO)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_INFO, notification)
    }
}
