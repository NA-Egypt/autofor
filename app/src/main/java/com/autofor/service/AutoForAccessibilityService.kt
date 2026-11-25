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
        private const val MAX_TRAVERSAL_DEPTH = 15
        private const val MAX_TRAVERSAL_NODES = 200

        var instance: AutoForAccessibilityService? = null
            private set

        @Volatile
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
    }

    private val handler = Handler(Looper.getMainLooper())
    private var repository: RuleRepository? = null
    private var pendingNavigationRunnable: Runnable? = null

    private fun getRepository(): RuleRepository {
        return repository ?: RuleRepository(this).also { repository = it }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true
        repository = RuleRepository(this)
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

        // Fast-path: never inspect UI or touch anything unless AutoFor is actively awaiting an MMI result.
        val repo = getRepository()
        if (!repo.isAwaitingMmi()) {
            return
        }

        val eventPkg = event.packageName?.toString()
        if (eventPkg != null && MmiDialogDetector.shouldIgnorePackage(eventPkg, eventPkg, packageName)) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        val rootPkg = rootNode.packageName?.toString()
        if (MmiDialogDetector.shouldIgnorePackage(eventPkg, rootPkg, packageName)) {
            return
        }

        val allTexts = mutableListOf<String>()
        var visitedNodes = 0
        extractAllTexts(rootNode, allTexts, depth = 0) {
            visitedNodes++
            visitedNodes <= MAX_TRAVERSAL_NODES
        }

        if (!MmiDialogDetector.isMmiResultDialog(allTexts)) {
            return
        }

        val relevantText = allTexts
            .filter { it.isNotBlank() && it.length > 2 }
            .distinct()
            .joinToString(". ")

        if (relevantText.isNotBlank()) {
            val statusText = if (relevantText.length > 120) relevantText.take(120) + "..." else relevantText
            repo.setLastForwardingStatus("Carrier: $statusText")
            repo.setCarrierStatus(statusText)
            repo.setLastExecutionTime(System.currentTimeMillis())
        }

        // Clear awaiting state before clicking/navigating so follow-up events are immediately ignored.
        repo.setAwaitingMmi(false)

        findAndClickDismissButton(rootNode)

        pendingNavigationRunnable?.let { handler.removeCallbacks(it) }
        val navRunnable = Runnable {
            pendingNavigationRunnable = null
            val shouldLock = lastWasLocked
            lastWasLocked = false
            if (shouldLock && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
        pendingNavigationRunnable = navRunnable
        handler.postDelayed(navRunnable, 500L)
    }

    private fun extractAllTexts(
        node: AccessibilityNodeInfo?,
        results: MutableList<String>,
        depth: Int,
        canVisitNext: () -> Boolean
    ) {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH || !canVisitNext()) return

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            results.add(text)
        }
        val contentDesc = node.contentDescription?.toString()?.trim()
        if (!contentDesc.isNullOrEmpty()) {
            results.add(contentDesc)
        }

        for (i in 0 until node.childCount) {
            extractAllTexts(node.getChild(i), results, depth + 1, canVisitNext)
        }
    }

    private fun findAndClickDismissButton(rootNode: AccessibilityNodeInfo): Boolean {
        // 1. Try finding standard dialog positive/dismiss button IDs
        val standardIds = listOf("android:id/button1", "button1", "ok_button", "dismiss_button")
        for (id in standardIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                val label = node.text?.toString()?.trim()?.lowercase(Locale.ROOT)
                if (label == "cancel" || label == "abort") continue
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
            }
        }

        // 2. Walk the tree for an exact dismissal button label ("OK", "Dismiss", "Close", "Done", etc.)
        var visited = 0
        return findExactLabelButton(rootNode, depth = 0) {
            visited++
            visited <= MAX_TRAVERSAL_NODES
        }
    }

    private fun findExactLabelButton(
        node: AccessibilityNodeInfo?,
        depth: Int,
        canVisitNext: () -> Boolean
    ): Boolean {
        if (node == null || depth > MAX_TRAVERSAL_DEPTH || !canVisitNext()) return false

        val label = node.text?.toString() ?: node.contentDescription?.toString()
        if (MmiDialogDetector.isSafeDismissButtonLabel(label)) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            var parent = node.parent
            var parentSteps = 0
            while (parent != null && parentSteps < 4) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                parent = parent.parent
                parentSteps++
            }
        }

        for (i in 0 until node.childCount) {
            if (findExactLabelButton(node.getChild(i), depth + 1, canVisitNext)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.w(TAG, "AutoForAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingNavigationRunnable?.let { handler.removeCallbacks(it) }
        pendingNavigationRunnable = null
        handler.removeCallbacksAndMessages(null)
        if (instance == this) instance = null
        isServiceRunning = false
        Log.d(TAG, "AutoForAccessibilityService destroyed")
    }
}
