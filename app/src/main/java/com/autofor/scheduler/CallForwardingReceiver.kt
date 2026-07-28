package com.autofor.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.autofor.R
import com.autofor.data.RuleRepository

class CallForwardingReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "autofor_notifications"
        const val CHANNEL_NAME = "AutoFor Status Notifications"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ScheduleManager.ACTION_TRIGGER_FORWARDING) {
            val enable = intent.getBooleanExtra(ScheduleManager.EXTRA_ENABLE_FORWARDING, false)
            val phoneNumber = intent.getStringExtra(ScheduleManager.EXTRA_PHONE_NUMBER) ?: ""

            executeCallForwarding(context, enable, phoneNumber)

            // Reschedule next occurrences
            ScheduleManager(context).rescheduleAll()
        }
    }

    private fun executeCallForwarding(context: Context, enable: Boolean, phoneNumber: String) {
        val repository = RuleRepository(context)

        val mmiCode = if (enable) {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            "*21*$cleanNumber#"
        } else {
            "#21#"
        }

        val encodedCode = Uri.encode(mmiCode)
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$encodedCode")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val statusText = if (enable) {
            "Activated call forwarding to $phoneNumber ($mmiCode)"
        } else {
            "Deactivated call forwarding ($mmiCode)"
        }

        repository.setLastForwardingStatus(statusText)

        try {
            context.startActivity(callIntent)
            sendNotification(context, "AutoFor Call Forwarding", statusText, callIntent)
        } catch (e: SecurityException) {
            val errorText = "Permission CALL_PHONE missing. Failed to execute $mmiCode"
            repository.setLastForwardingStatus(errorText)
            sendNotification(context, "AutoFor Permission Error", errorText, null)
        } catch (e: Exception) {
            // Android 10+ background activity launch restriction or generic error fallback
            val errorText = "Forwarding trigger ready: $mmiCode. Tap notification action to complete dial."
            repository.setLastForwardingStatus(errorText)
            sendNotification(context, "AutoFor Scheduled Forwarding", errorText, callIntent)
        }
    }

    private fun sendNotification(context: Context, title: String, message: String, callIntent: Intent?) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = callIntent?.let {
            android.app.PendingIntent.getActivity(
                context,
                0,
                it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
            builder.addAction(android.R.drawable.ic_menu_call, "Dial MMI Code Now", pendingIntent)
        }

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }

}
