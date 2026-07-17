package ir.lbo.locationsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.util.regex.Pattern

class ViewerSmsReceiver : BroadcastReceiver() {

    // Matches "lat, lng" pairs like the ones the tracker app sends,
    // e.g. "Location: 35.715298, 51.404343"
    private val locationPattern: Pattern =
        Pattern.compile("([-+]?\\d{1,3}\\.\\d+),\\s*([-+]?\\d{1,3}\\.\\d+)")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }.trim()

        val settings = SettingsRepository(context)
        val trackerPhone = settings.getTrackerViewerPhone()

        val matched = !trackerPhone.isNullOrBlank() && PhoneUtils.isAllowed(sender, listOf(trackerPhone))

        // Only record messages coming from the configured tracker phone number.
        if (!matched) return

        var latitude: Double? = null
        var longitude: Double? = null
        val matcher = locationPattern.matcher(fullBody)
        if (matcher.find()) {
            latitude = matcher.group(1)?.toDoubleOrNull()
            longitude = matcher.group(2)?.toDoubleOrNull()
        }

        ViewerHistoryStore.addEntry(
            context,
            ViewerHistoryEntry(
                timestamp = System.currentTimeMillis(),
                rawText = fullBody,
                latitude = latitude,
                longitude = longitude
            )
        )

        NewMessageNotifier.notifyListeners()
    }
}
