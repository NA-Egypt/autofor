package com.autofor.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MmiDialogDetectorTest {

    private val ownPkg = "com.autofor"

    @Test
    fun shouldIgnorePackage_ignoresOwnAppAndSettingsAndLaunchers() {
        assertTrue(MmiDialogDetector.shouldIgnorePackage(ownPkg, ownPkg, ownPkg))
        assertTrue(MmiDialogDetector.shouldIgnorePackage("com.android.phone", ownPkg, ownPkg))
        assertTrue(MmiDialogDetector.shouldIgnorePackage("com.android.settings", "com.android.settings", ownPkg))
        assertTrue(MmiDialogDetector.shouldIgnorePackage("com.samsung.android.settings", null, ownPkg))
        assertTrue(MmiDialogDetector.shouldIgnorePackage("com.coloros.settings", null, ownPkg))
        assertTrue(MmiDialogDetector.shouldIgnorePackage("com.android.systemui", null, ownPkg))
        assertTrue(MmiDialogDetector.shouldIgnorePackage("com.google.android.apps.nexuslauncher", null, ownPkg))
        assertTrue(MmiDialogDetector.shouldIgnorePackage(null, null, ownPkg))
    }

    @Test
    fun shouldIgnorePackage_allowsPhoneAndTelecomDialogs() {
        assertFalse(MmiDialogDetector.shouldIgnorePackage("com.android.phone", "com.android.phone", ownPkg))
        assertFalse(MmiDialogDetector.shouldIgnorePackage("android", "android", ownPkg))
        assertFalse(MmiDialogDetector.shouldIgnorePackage("com.google.android.dialer", "com.android.phone", ownPkg))
        assertFalse(MmiDialogDetector.shouldIgnorePackage("com.samsung.android.incallui", null, ownPkg))
    }

    @Test
    fun isMmiResultDialog_matchesRealCarrierResponses() {
        assertTrue(
            MmiDialogDetector.isMmiResultDialog(
                listOf("Call forwarding", "Registration was successful.", "OK")
            )
        )
        assertTrue(
            MmiDialogDetector.isMmiResultDialog(
                listOf("Call forwarding", "Erasure was successful.", "OK")
            )
        )
        assertTrue(
            MmiDialogDetector.isMmiResultDialog(
                listOf("Call forwarding", "Voice: +1234567890", "Data: Not forwarded", "OK")
            )
        )
        assertTrue(
            MmiDialogDetector.isMmiResultDialog(
                listOf("Connection problem or invalid MMI code.", "OK")
            )
        )
        assertTrue(
            MmiDialogDetector.isMmiResultDialog(
                listOf("Service has been disabled.", "OK")
            )
        )
    }

    @Test
    fun isMmiResultDialog_ignoresProgressDialogsAndGenericScreens() {
        assertFalse(MmiDialogDetector.isMmiResultDialog(listOf("USSD code running…", "Cancel")))
        assertFalse(MmiDialogDetector.isMmiResultDialog(listOf("MMI code started")))
        assertFalse(MmiDialogDetector.isMmiResultDialog(listOf("Call forwarding", "Please wait…")))
        assertFalse(MmiDialogDetector.isMmiResultDialog(listOf("Forwarded message from Alex", "Forwarding to team")))
        assertFalse(MmiDialogDetector.isMmiResultDialog(listOf("Carrier settings", "SIM 1", "OK")))
        assertFalse(MmiDialogDetector.isMmiResultDialog(emptyList()))
    }

    @Test
    fun isSafeDismissButtonLabel_matchesOnlyExactDismissLabels() {
        assertTrue(MmiDialogDetector.isSafeDismissButtonLabel("OK"))
        assertTrue(MmiDialogDetector.isSafeDismissButtonLabel("  ok "))
        assertTrue(MmiDialogDetector.isSafeDismissButtonLabel("Dismiss"))
        assertTrue(MmiDialogDetector.isSafeDismissButtonLabel("Close"))
        assertTrue(MmiDialogDetector.isSafeDismissButtonLabel("Done"))
        assertTrue(MmiDialogDetector.isSafeDismissButtonLabel("Got it"))
        assertTrue(MmiDialogDetector.isSafeDismissButtonLabel("Accept"))

        assertFalse(MmiDialogDetector.isSafeDismissButtonLabel("Cancel"))
        assertFalse(MmiDialogDetector.isSafeDismissButtonLabel("Book"))
        assertFalse(MmiDialogDetector.isSafeDismissButtonLabel("Look"))
        assertFalse(MmiDialogDetector.isSafeDismissButtonLabel("Forward Now"))
        assertFalse(MmiDialogDetector.isSafeDismissButtonLabel(null))
        assertFalse(MmiDialogDetector.isSafeDismissButtonLabel(""))
    }
}
