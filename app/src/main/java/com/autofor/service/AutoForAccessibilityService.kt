package com.autofor.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.autofor.data.RuleRepository
import java.util.Locale

class AutoForAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoForAccessibility"
        const val EXTRA_WAS_LOCKED = "extra_was_locked"

        var instance: AutoForAccessibilityService? = null
            private set

        var isServiceRunning: Boolean = false
            private set

        @Volatile
        var lastWasLocked: Boolean = false

        fun triggerForwarding(context: Context, enable: Boolean, phoneNumber: String, wasLocked: Boolean) {
            lastWasLocked = wasLocked
            val activityIntent = Intent(context, com.autofor.scheduler.ForwardingActivity::class.java).apply {
                action = com.autofor.scheduler.ScheduleManager.ACTION_TRIGGER_FORWARDING
                putExtra(com.autofor.scheduler.ScheduleManager.EXTRA_ENABLE_FORWARDING, enable)
                putExtra(com.autofor.scheduler.ScheduleManager.EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_WAS_LOCKED, wasLocked)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val svc = instance
            if (svc != null) {
                svc.startActivity(activityIntent)
            } else {
                context.startActivity(activityIntent)
            }
        }

        private val MMI_KEYWORDS = listOf(
            "call forwarding",
            "forwarding",
            "registration was successful",
            "deactivation was successful",
            "erasure was successful",
            "service was enabled",
            "service has been disabled",
            "service has been enabled",
            "mmi",
            "ussd",
            "unconditional",
            "carrier",
            "voice:",
            "data:",
            "fax:",
            "not forwarded"
        )
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true
        Log.d(TAG, "AutoForAccessibilityService connected and ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        val repository = RuleRepository(this)
        val isAwaiting = repository.isAwaitingMmi()

        // Check if current window or node tree contains carrier / MMI response content
        val allTexts = mutableListOf<String>()
        extractAllTexts(rootNode, allTexts)

        val fullText = allTexts.joinToString(" ")
        val lowerText = fullText.lowercase(Locale.ROOT)

        val containsMmiKeyword = MMI_KEYWORDS.any { keyword -> lowerText.contains(keyword) }

        if (isAwaiting || containsMmiKeyword) {
            val relevantText = allTexts
                .filter { it.isNotBlank() && it.length > 2 }
                .distinct()
                .joinToString(". ")

            if (relevantText.isNotBlank()) {
                val statusText = if (relevantText.length > 120) relevantText.take(120) + "..." else relevantText
                repository.setLastForwardingStatus("Carrier: $statusText")
                repository.setCarrierStatus(statusText)
                repository.setLastExecutionTime(System.currentTimeMillis())
            }

            // Attempt to locate and click the dismissal button (OK / Dismiss)
            val clicked = findAndClickPositiveButton(rootNode)
            if (clicked || containsMmiKeyword) {
                repository.setAwaitingMmi(false)

                // Return to home or re-lock screen after brief delay to allow system click to process
                handler.postDelayed({
                    if (lastWasLocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    } else {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }
                    lastWasLocked = false
                }, 500L)
            }
        }
    }

    private fun extractAllTexts(node: AccessibilityNodeInfo?, results: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            results.add(text)
        }
        val contentDesc = node.contentDescription?.toString()?.trim()
        if (!contentDesc.isNullOrEmpty()) {
            results.add(contentDesc)
        }

        for (i in 0 until node.childCount) {
            extractAllTexts(node.getChild(i), results)
        }
    }

    private fun findAndClickPositiveButton(rootNode: AccessibilityNodeInfo): Boolean {
        // 1. Try finding standard dialog button IDs
        val standardIds = listOf("android:id/button1", "button1", "ok_button", "dismiss_button")
        for (id in standardIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
            }
        }

        // 2. Try finding button by text ("OK", "Dismiss", "Close", "Done", "Accept")
        val buttonTexts = listOf("OK", "Ok", "ok", "Dismiss", "Close", "Done", "Accept")
        for (btnText in buttonTexts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(btnText)
            for (node in nodes) {
                if (node.isClickable) {
                    if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                } else {
                    // Check if parent is clickable
                    var parent = node.parent
                    while (parent != null) {
                        if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            return true
                        }
                        parent = parent.parent
                    }
                }
            }
        }

        // 3. Fallback: Search for any clickable Button node in dialog
        return findFirstClickableButton(rootNode)
    }

    private fun findFirstClickableButton(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.className?.toString()?.contains("Button", ignoreCase = true) == true && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (i in 0 until node.childCount) {
            if (findFirstClickableButton(node.getChild(i))) return true
        }
        return false
    }

    override fun onInterrupt() {
        Log.w(TAG, "AutoForAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        isServiceRunning = false
        Log.d(TAG, "AutoForAccessibilityService destroyed")
    }
}
