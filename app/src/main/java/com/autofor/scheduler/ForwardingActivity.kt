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
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        keyguardManager?.requestDismissKeyguard(this, null)

        val wasLocked = intent.getBooleanExtra(com.autofor.service.AutoForAccessibilityService.EXTRA_WAS_LOCKED, false)
        if (wasLocked) {
            com.autofor.service.AutoForAccessibilityService.lastWasLocked = true
        }

        // Cancel any pending alarm heads-up notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CallForwardingReceiver.NOTIFICATION_ID_URGENT)

        if (intent.action == ACTION_CHECK_STATUS) {
            executeCheckStatus(this)
        } else {
            val enable = intent.getBooleanExtra(ScheduleManager.EXTRA_ENABLE_FORWARDING, false)
            val phoneNumber = intent.getStringExtra(ScheduleManager.EXTRA_PHONE_NUMBER) ?: ""
            executeCallForwarding(this, enable, phoneNumber)
            // Reschedule next occurrences after triggering
            ScheduleManager(this).rescheduleAll()
        }

        finish()
    }

    companion object {
        const val ACTION_CHECK_STATUS = "com.autofor.ACTION_CHECK_STATUS"

        fun executeCallForwarding(context: Context, enable: Boolean, phoneNumber: String): Boolean {
            val repository = RuleRepository(context)

            // Standard 3GPP GSM code: **21* for registration & activation, ##21# for erasure & cancellation
            val mmiCode = if (enable) {
                val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
                "**21*$cleanNumber#"
            } else {
                "##21#"
            }

            return dialMmiCode(context, mmiCode, repository, if (enable) "forwarding to $phoneNumber" else "forwarding cancellation")
        }

        fun executeCheckStatus(context: Context): Boolean {
            val repository = RuleRepository(context)
            val mmiCode = "*#21#"
            return dialMmiCode(context, mmiCode, repository, "status check (*#21#)")
        }

        private fun dialMmiCode(context: Context, mmiCode: String, repository: RuleRepository, actionDescription: String): Boolean {
            repository.setAwaitingMmi(true, mmiCode)
            repository.setLastExecutionTime(System.currentTimeMillis())

            val encodedCode = Uri.encode(mmiCode)
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$encodedCode")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return try {
                context.startActivity(callIntent)
                val statusText = "Dialed $actionDescription ($mmiCode)"
                repository.setLastForwardingStatus(statusText)
                repository.setLastForwardingError(null)
                true
            } catch (e: SecurityException) {
                repository.setAwaitingMmi(false)
                val errorText = "Permission CALL_PHONE missing. Failed to execute $mmiCode"
                repository.setLastForwardingStatus(errorText)
                repository.setLastForwardingError(errorText)
                false
            } catch (e: Exception) {
                repository.setAwaitingMmi(false)
                val errorText = "Failed to execute $mmiCode: ${e.localizedMessage ?: e.javaClass.simpleName}"
                repository.setLastForwardingStatus(errorText)
                repository.setLastForwardingError(errorText)
                false
            }
        }
    }
}
