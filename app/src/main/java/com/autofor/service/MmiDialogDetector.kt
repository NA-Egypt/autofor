package com.autofor.service

import com.autofor.scheduler.MmiCodes
import java.util.Locale

/**
 * Pure detection helpers for identifying genuine carrier MMI/USSD result dialogs
 * and matching safe dismissal button labels without touching unrelated UI screens.
 */
object MmiDialogDetector {

    private val IGNORED_PACKAGES = setOf(
        "com.android.settings",
        "com.samsung.android.settings",
        "com.miui.securitycenter",
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.oppo.launcher",
        "com.oneplus.launcher"
    )

    private val RESULT_KEYWORDS = listOf(
        "call forwarding",
        "registration was successful",
        "registration successful",
        "deactivation was successful",
        "deactivation successful",
        "erasure was successful",
        "erasure successful",
        "service was enabled",
        "service has been enabled",
        "service was disabled",
        "service has been disabled",
        "connection problem or invalid mmi code",
        "invalid mmi code",
        "unconditional",
        "not forwarded",
        "forwarded to",
        "voice:",
        "data:",
        "fax:",
        "sms:",
        "sync:",
        "async:",
        "packet:",
        "pad:",
        "mmi complete"
    )

    private val PROGRESS_PHRASES = listOf(
        "ussd code running",
        "mmi code started",
        "please wait"
    )

    private val COMPLETION_PHRASES = listOf(
        "successful",
        "enabled",
        "disabled",
        "invalid",
        "problem",
        "error",
        "failed",
        "not forwarded",
        "forwarded to",
        "voice:",
        "data:",
        "fax:",
        "sms:",
        "sync:",
        "async:",
        "packet:",
        "pad:",
        "mmi complete"
    )

    val DISMISS_BUTTON_LABELS = setOf(
        "ok",
        "dismiss",
        "close",
        "done",
        "got it",
        "accept"
    )

    fun shouldIgnorePackage(
        eventPackage: String?,
        rootPackage: String?,
        ownPackage: String
    ): Boolean {
        val pkgs = listOfNotNull(eventPackage, rootPackage)
        if (pkgs.isEmpty()) return true
        return pkgs.any { pkg ->
            val lower = pkg.lowercase(Locale.ROOT)
            pkg == ownPackage ||
                lower in IGNORED_PACKAGES ||
                lower.contains(".settings") ||
                lower.endsWith("settings") ||
                lower.contains("launcher")
        }
    }

    fun isMmiResultDialog(texts: List<String>): Boolean {
        if (texts.isEmpty()) return false
        val lowerText = texts.joinToString(" ").lowercase(Locale.ROOT)
        val hasProgressPhrase = PROGRESS_PHRASES.any { lowerText.contains(it) }
        val hasCompletionPhrase = COMPLETION_PHRASES.any { lowerText.contains(it) }
        if (hasProgressPhrase && !hasCompletionPhrase) {
            return false
        }
        return RESULT_KEYWORDS.any { lowerText.contains(it) }
    }

    fun isSafeDismissButtonLabel(label: String?): Boolean {
        if (label.isNullOrBlank()) return false
        return label.trim().lowercase(Locale.ROOT) in DISMISS_BUTTON_LABELS
    }

    /** Resource-ID names of the call button on the keypad of common dialer apps. */
    val DIALER_CALL_BUTTON_ID_NAMES = listOf(
        "dialpad_voice_call_button",
        "dialpad_floating_action_button",
        "dialButton",
        "dial_button",
        "call_button"
    )

    private val CALL_BUTTON_LABELS = setOf("call", "dial", "voice call")

    /** Words in a carrier reply that mean the request was not carried out. */
    private val FAILURE_PHRASES = listOf(
        "invalid",
        "problem",
        "error",
        "failed",
        "unsuccessful",
        "rejected",
        "not allowed",
        "not supported"
    )

    private fun normalizeDialString(text: String): String = text.filter { it.isDigit() || it in "*#+" }

    /**
     * True when the screen shows [pendingCode] typed into a dialer keypad, which happens when the
     * dialer opens the code for confirmation instead of sending it.
     */
    fun isPendingCodeInDialer(texts: List<String>, pendingCode: String?): Boolean {
        if (pendingCode.isNullOrBlank()) return false
        val target = normalizeDialString(pendingCode)
        if (target.length < 4) return false
        return texts.any { normalizeDialString(it) == target && it.trim().length <= target.length + 4 }
    }

    fun isCallButtonLabel(label: String?): Boolean {
        if (label.isNullOrBlank()) return false
        return label.trim().lowercase(Locale.ROOT) in CALL_BUTTON_LABELS
    }

    /**
     * What a carrier reply confirms about forwarding after [code] was sent: the cleaned target number
     * for an activation (**21*X#), "" for a cancellation (##21#), or null when the reply reports a
     * failure or the code doesn't change forwarding (such as the *#21# status query).
     */
    fun confirmedForwardingNumber(code: String?, replyTexts: List<String>): String? {
        if (code.isNullOrBlank()) return null
        val reply = replyTexts.joinToString(" ").lowercase(Locale.ROOT)
        if (FAILURE_PHRASES.any { reply.contains(it) }) return null
        return when {
            code == MmiCodes.CANCEL_UNCONDITIONAL -> ""
            code.startsWith("**21*") && code.endsWith("#") ->
                code.removePrefix("**21*").removeSuffix("#").takeIf { it.isNotEmpty() }
            else -> null
        }
    }
}
