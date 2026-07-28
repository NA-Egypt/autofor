package com.autofor.scheduler

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.autofor.data.RuleRepository

class ForwardingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enable = intent.getBooleanExtra(ScheduleManager.EXTRA_ENABLE_FORWARDING, false)
        val phoneNumber = intent.getStringExtra(ScheduleManager.EXTRA_PHONE_NUMBER) ?: ""

        executeCallForwarding(this, enable, phoneNumber)

        // Reschedule next occurrences after triggering
        ScheduleManager(this).rescheduleAll()

        finish()
    }

    companion object {
        fun executeCallForwarding(context: Context, enable: Boolean, phoneNumber: String) {
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
            } catch (e: SecurityException) {
                val errorText = "Permission CALL_PHONE missing. Failed to execute $mmiCode"
                repository.setLastForwardingStatus(errorText)
            } catch (e: Exception) {
                val errorText = "Failed to execute $mmiCode: ${e.localizedMessage}"
                repository.setLastForwardingStatus(errorText)
            }
        }
    }
}
