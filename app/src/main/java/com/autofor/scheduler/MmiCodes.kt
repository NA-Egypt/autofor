package com.autofor.scheduler

/** Standard 3GPP call-forwarding codes: **21* registers and activates, ##21# erases, *#21# queries. */
object MmiCodes {
    const val CANCEL_UNCONDITIONAL = "##21#"
    const val QUERY_UNCONDITIONAL = "*#21#"

    fun cleanNumber(phoneNumber: String): String = phoneNumber.replace(Regex("[^0-9+]"), "")

    fun enableUnconditional(phoneNumber: String): String = "**21*${cleanNumber(phoneNumber)}#"

    fun forState(enable: Boolean, phoneNumber: String): String =
        if (enable) enableUnconditional(phoneNumber) else CANCEL_UNCONDITIONAL
}
