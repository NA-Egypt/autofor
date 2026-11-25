package com.autofor.service

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
}
