package com.autofor.scheduler

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import com.autofor.data.RuleRepository

class ForwardingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow display and screen turn-on over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        keyguardManager?.requestDismissKeyguard(this, null)

        val enable = intent.getBooleanExtra(ScheduleManager.EXTRA_ENABLE_FORWARDING, false)
        val phoneNumber = intent.getStringExtra(ScheduleManager.EXTRA_PHONE_NUMBER) ?: ""

        // Cancel any pending alarm heads-up notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CallForwardingReceiver.NOTIFICATION_ID_URGENT)

        executeCallForwarding(this, enable, phoneNumber)

        // Reschedule next occurrences after triggering
        ScheduleManager(this).rescheduleAll()

        finish()
    }

    companion object {
        fun executeCallForwarding(context: Context, enable: Boolean, phoneNumber: String): Boolean {
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

            return try {
                context.startActivity(callIntent)
                val statusText = if (enable) {
                    "Successfully initiated forwarding to $phoneNumber ($mmiCode)"
                } else {
                    "Successfully initiated forwarding cancellation ($mmiCode)"
                }
                repository.setLastForwardingStatus(statusText)
                true
            } catch (e: SecurityException) {
                val errorText = "Permission CALL_PHONE missing. Failed to execute $mmiCode"
                repository.setLastForwardingStatus(errorText)
                false
            } catch (e: Exception) {
                val errorText = "Failed to execute $mmiCode: ${e.localizedMessage ?: e.javaClass.simpleName}"
                repository.setLastForwardingStatus(errorText)
                false
            }
        }
    }
}
