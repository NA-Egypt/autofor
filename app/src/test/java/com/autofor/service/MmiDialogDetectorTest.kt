package com.autofor.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun isPendingCodeInDialer_matchesOnlyTheExactPendingCode() {
        // Text seen on Google Dialer's keypad when it holds the code instead of sending it.
        val keypad = listOf("Create new contact", "Add to a contact", "**21*01024528918#", "1", "2", "ABC")
        assertTrue(MmiDialogDetector.isPendingCodeInDialer(keypad, "**21*01024528918#"))
        assertTrue(MmiDialogDetector.isPendingCodeInDialer(listOf("*#21#"), "*#21#"))
        assertFalse(MmiDialogDetector.isPendingCodeInDialer(keypad, "**21*01004065404#"))
        assertFalse(MmiDialogDetector.isPendingCodeInDialer(keypad, "##21#"))
        assertFalse(MmiDialogDetector.isPendingCodeInDialer(keypad, null))
        // A longer message that merely mentions the code is not the keypad.
        assertFalse(MmiDialogDetector.isPendingCodeInDialer(listOf("Dialed forwarding to 01024528918 (**21*01024528918#)"), "**21*01024528918#"))
    }

    @Test
    fun isCallButtonLabel_matchesExactLabelsOnly() {
        assertTrue(MmiDialogDetector.isCallButtonLabel("Call"))
        assertTrue(MmiDialogDetector.isCallButtonLabel(" dial "))
        assertFalse(MmiDialogDetector.isCallButtonLabel("Call history"))
        assertFalse(MmiDialogDetector.isCallButtonLabel(null))
    }

    @Test
    fun confirmedForwardingNumber_readsActivationCancellationAndFailure() {
        val ok = listOf("Vodafone Message. Call forwarding", "Voice: +201024528918")
        assertEquals("01024528918", MmiDialogDetector.confirmedForwardingNumber("**21*01024528918#", ok))
        assertEquals("", MmiDialogDetector.confirmedForwardingNumber("##21#", listOf("Erasure was successful")))
        assertNull(MmiDialogDetector.confirmedForwardingNumber("*#21#", ok))
        assertNull(MmiDialogDetector.confirmedForwardingNumber("**21*01024528918#", listOf("Connection problem or invalid MMI code.")))
        assertNull(MmiDialogDetector.confirmedForwardingNumber(null, ok))
    }
}
