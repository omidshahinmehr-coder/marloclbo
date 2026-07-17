package ir.lbo.locationsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SmsReceiver : BroadcastReceiver() {

    private val autosendOnPattern = Regex("(?i)^autosend\\s+on\\s+(\\d+)$")
    private val moveAlertOnPattern = Regex("(?i)^MoveAlert\\s+(\\d+)\\s+on$")
    private val geofenceOnPattern = Regex("(?i)^Geofence\\s+on\\s+(\\d+)$")
    private val lowBatteryOnPattern = Regex("(?i)^LowbatteryAlert\\s+(\\d+)\\s+on$")
    private val setAdminNumbersPattern = Regex("(?i)^SetAdminNumbers\\s+(.+)$")
    private val setAutosendNumbersPattern = Regex("(?i)^SetAutosendNumbers\\s+(.+)$")
    private val setLogTimerPattern = Regex("(?i)^SetLogTimer\\s+(\\d+)$")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }.trim()

        val settings = SettingsRepository(context)
        val allowedNumbers = settings.getAllowedNumbersList()

        // All remote commands require the sender to be in the whitelist.
        // If the list is empty, nothing is trusted yet.
        if (!PhoneUtils.isAllowed(sender, allowedNumbers)) return

        val autosendOnMatch = autosendOnPattern.find(fullBody)
        val moveAlertOnMatch = moveAlertOnPattern.find(fullBody)
        val geofenceOnMatch = geofenceOnPattern.find(fullBody)
        val lowBatteryOnMatch = lowBatteryOnPattern.find(fullBody)
        val setAdminNumbersMatch = setAdminNumbersPattern.find(fullBody)
        val setAutosendNumbersMatch = setAutosendNumbersPattern.find(fullBody)
        val setLogTimerMatch = setLogTimerPattern.find(fullBody)

        when {
            fullBody.equals("sendloc", ignoreCase = true) -> {
                enqueueLocationReply(context, sender)
            }
            autosendOnMatch != null -> {
                val minutes = autosendOnMatch.groupValues[1].toLongOrNull() ?: 15L
                handleAutoSendOn(context, settings, sender, minutes)
            }
            fullBody.equals("autosend off", ignoreCase = true) -> {
                handleAutoSendOff(context, settings, sender)
            }
            fullBody.equals("sendlog", ignoreCase = true) -> {
                enqueueSendLogEmail(context, sender)
            }
            fullBody.equals("dellog", ignoreCase = true) -> {
                handleDeleteLogs(context, sender)
            }
            fullBody.equals("ping", ignoreCase = true) -> {
                handlePing(context, sender)
            }
            moveAlertOnMatch != null -> {
                val distance = moveAlertOnMatch.groupValues[1].toLongOrNull() ?: 50L
                handleMoveAlertOn(context, settings, sender, distance)
            }
            fullBody.equals("MoveAlert off", ignoreCase = true) -> {
                handleMoveAlertOff(context, settings, sender)
            }
            geofenceOnMatch != null -> {
                val radius = geofenceOnMatch.groupValues[1].toLongOrNull() ?: 500L
                handleGeofenceOn(context, settings, sender, radius)
            }
            fullBody.equals("Geofence off", ignoreCase = true) -> {
                handleGeofenceOff(context, settings, sender)
            }
            lowBatteryOnMatch != null -> {
                val percent = lowBatteryOnMatch.groupValues[1].toLongOrNull() ?: 15L
                handleLowBatteryOn(context, settings, sender, percent)
            }
            fullBody.equals("LowbatteryAlert off", ignoreCase = true) -> {
                handleLowBatteryOff(context, settings, sender)
            }
            setAdminNumbersMatch != null -> {
                handleSetAdminNumbers(context, settings, sender, setAdminNumbersMatch.groupValues[1].trim())
            }
            setAutosendNumbersMatch != null -> {
                handleSetAutosendNumbers(context, settings, sender, setAutosendNumbersMatch.groupValues[1].trim())
            }
            setLogTimerMatch != null -> {
                val minutes = setLogTimerMatch.groupValues[1].toLongOrNull() ?: 15L
                handleSetLogTimer(context, settings, sender, minutes)
            }
        }
    }

    private fun enqueueLocationReply(context: Context, sender: String) {
        val data = Data.Builder()
            .putString(ReplyLocationWorker.KEY_PHONE, sender)
            .build()

        val request = OneTimeWorkRequestBuilder<ReplyLocationWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    private fun handleAutoSendOn(
        context: Context,
        settings: SettingsRepository,
        sender: String,
        requestedMinutes: Long
    ) {
        val interval = if (requestedMinutes < 15) 15 else requestedMinutes
        settings.saveIntervalMinutes(interval)
        settings.saveAutoSendEnabled(true)
        WorkScheduler.schedule(context, interval)

        replyText(context, sender, "ارسال خودکار موقعیت هر $interval دقیقه فعال شد.")
    }

    private fun handleAutoSendOff(context: Context, settings: SettingsRepository, sender: String) {
        settings.saveAutoSendEnabled(false)
        WorkScheduler.cancel(context)

        replyText(context, sender, "ارسال خودکار موقعیت غیرفعال شد.")
    }

    private fun enqueueSendLogEmail(context: Context, sender: String) {
        val data = Data.Builder()
            .putString(SendLogEmailWorker.KEY_REPLY_PHONE, sender)
            .build()

        val request = OneTimeWorkRequestBuilder<SendLogEmailWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    private fun handleDeleteLogs(context: Context, sender: String) {
        val deletedCount = LocationLogger.deleteArchivedLogFiles(context)
        replyText(context, sender, "$deletedCount فایل لاگ آرشیو‌شده حذف شد.")
    }

    private fun handlePing(context: Context, sender: String) {
        val battery = BatteryHelper.getBatteryPercent(context)
        val batteryText = if (battery in 0..100) "$battery٪" else "نامشخص"
        replyText(context, sender, "ردیاب فعال و آماده است. باتری: $batteryText")
    }

    private fun handleMoveAlertOn(context: Context, settings: SettingsRepository, sender: String, requestedDistance: Long) {
        val distance = if (requestedDistance < 0) 0 else requestedDistance
        settings.saveMinLogDistanceMeters(distance)
        settings.saveMovementAlertEnabled(true)
        replyText(context, sender, "اعلام میزان جابجایی فعال شد (حداقل $distance متر).")
    }

    private fun handleMoveAlertOff(context: Context, settings: SettingsRepository, sender: String) {
        settings.saveMovementAlertEnabled(false)
        replyText(context, sender, "اعلام میزان جابجایی غیرفعال شد.")
    }

    private fun handleGeofenceOn(context: Context, settings: SettingsRepository, sender: String, requestedRadius: Long) {
        val radius = if (requestedRadius < 50) 50 else requestedRadius
        settings.saveGeofenceRadiusMeters(radius)

        val data = Data.Builder()
            .putString(GeofenceEnableWorker.KEY_PHONE, sender)
            .build()

        val request = OneTimeWorkRequestBuilder<GeofenceEnableWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    private fun handleGeofenceOff(context: Context, settings: SettingsRepository, sender: String) {
        settings.saveGeofenceEnabled(false)
        replyText(context, sender, "حصار جغرافیایی غیرفعال شد.")
    }

    private fun handleLowBatteryOn(context: Context, settings: SettingsRepository, sender: String, requestedPercent: Long) {
        val percent = requestedPercent.coerceIn(1L, 90L)
        settings.saveBatteryAlertThreshold(percent)
        settings.saveBatteryAlertEnabled(true)
        replyText(context, sender, "هشدار باتری کم فعال شد (آستانه $percent٪).")
    }

    private fun handleLowBatteryOff(context: Context, settings: SettingsRepository, sender: String) {
        settings.saveBatteryAlertEnabled(false)
        replyText(context, sender, "هشدار باتری کم غیرفعال شد.")
    }

    private fun handleSetAdminNumbers(context: Context, settings: SettingsRepository, sender: String, numbersRaw: String) {
        settings.saveAllowedNumbersRaw(numbersRaw)
        val count = settings.getAllowedNumbersList().size
        replyText(context, sender, "شماره‌های مجاز به‌روزرسانی شد ($count شماره).")
    }

    private fun handleSetAutosendNumbers(context: Context, settings: SettingsRepository, sender: String, numbersRaw: String) {
        settings.savePhoneNumbersRaw(numbersRaw)
        val count = settings.getPhoneNumbersList().size
        replyText(context, sender, "شماره‌های ارسال خودکار به‌روزرسانی شد ($count شماره).")
    }

    private fun handleSetLogTimer(context: Context, settings: SettingsRepository, sender: String, requestedMinutes: Long) {
        val minutes = if (requestedMinutes < 15) 15 else requestedMinutes
        settings.saveLogIntervalMinutes(minutes)
        LogWorkScheduler.schedule(context, minutes)
        replyText(context, sender, "بازه ذخیره لاگ به $minutes دقیقه تغییر کرد.")
    }

    private fun replyText(context: Context, phone: String, text: String) {
        val data = Data.Builder()
            .putString(ReplyTextWorker.KEY_PHONE, phone)
            .putString(ReplyTextWorker.KEY_TEXT, text)
            .build()

        val request = OneTimeWorkRequestBuilder<ReplyTextWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
