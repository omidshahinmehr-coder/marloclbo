package ir.lbo.locationsms

import android.content.Context
import android.telephony.TelephonyManager

/**
 * Detects likely SIM card swaps. Android 10+ blocks regular apps from
 * reading true SIM identifiers (ICCID/IMEI/subscriber ID) for privacy
 * reasons, so this uses the SIM's carrier name + country code as a
 * best-effort fingerprint instead. It will NOT catch a swap between two
 * SIMs from the exact same carrier and country, but it does catch the
 * common case of a thief inserting their own (different-carrier) SIM.
 */
object SimWatcher {

    fun getCurrentFingerprint(context: Context): String {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val operator = telephonyManager?.simOperatorName?.trim() ?: ""
        val country = telephonyManager?.simCountryIso?.trim() ?: ""
        return "$operator|$country"
    }

    /**
     * Compares the current SIM fingerprint against the stored baseline.
     * Always updates the stored baseline. Returns true only when a
     * previous baseline existed AND it differs from the current one
     * (i.e. a real change, not the first-ever check).
     */
    fun checkAndUpdate(context: Context): Boolean {
        val current = getCurrentFingerprint(context)
        if (current.isBlank() || current == "|") return false // no SIM info available right now

        val settings = SettingsRepository(context)
        val previous = settings.getSimFingerprint()
        settings.saveSimFingerprint(current)

        return !previous.isNullOrBlank() && previous != current
    }
}
