package ir.lbo.locationsms

import android.content.Context
import android.location.Location
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

class LocationLogWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Cheap checks that don't need a GPS fix run first, so they still
        // happen even if the location fetch below fails or times out.
        checkSimChange(applicationContext)
        checkBatteryLevel(applicationContext)

        val location = LocationHelper.getCurrentLocation(applicationContext)
            ?: return Result.retry()

        checkGeofence(applicationContext, location)
        maybeLogLocation(applicationContext, location)

        return Result.success()
    }

    private fun maybeLogLocation(context: Context, location: Location) {
        val settings = SettingsRepository(context)
        val minDistance = settings.getMinLogDistanceMeters()

        if (minDistance > 0) {
            val last = settings.getLastLoggedLocation()
            if (last != null) {
                val results = FloatArray(1)
                Location.distanceBetween(last.first, last.second, location.latitude, location.longitude, results)
                val distanceMoved = results[0]

                if (distanceMoved < minDistance) {
                    // Hasn't moved far enough yet — skip writing a new row.
                    return
                }

                // Moved beyond the minimum distance: optionally notify by SMS.
                if (settings.isMovementAlertEnabled()) {
                    val roundedDistance = distanceMoved.toInt()
                    broadcastToNumbers(
                        context,
                        settings.getPhoneNumbersList(),
                        "خودرو حداقل $roundedDistance متر جابجا شد."
                    )
                }
            }
        }

        LocationLogger.appendEntry(context, location)
        settings.saveLastLoggedLocation(location.latitude, location.longitude)
    }

    private fun checkSimChange(context: Context) {
        val changed = SimWatcher.checkAndUpdate(context)
        if (!changed) return

        val settings = SettingsRepository(context)
        broadcastToNumbers(
            context,
            settings.getAllowedNumbersList(),
            "هشدار: سیم‌کارت گوشیِ ردیاب تغییر کرده است! اپراتور فعلی: ${SimWatcher.getCurrentFingerprint(context)}"
        )
    }

    private fun checkGeofence(context: Context, location: Location) {
        val settings = SettingsRepository(context)
        if (!settings.isGeofenceEnabled()) return

        val center = settings.getGeofenceCenter() ?: return
        val radius = settings.getGeofenceRadiusMeters()

        val results = FloatArray(1)
        Location.distanceBetween(center.first, center.second, location.latitude, location.longitude, results)
        val distance = results[0]

        val newState = if (distance <= radius) "inside" else "outside"
        val previousState = settings.getGeofenceState()
        settings.saveGeofenceState(newState)

        if (previousState == null || previousState == newState) return // no real transition yet

        val message = if (newState == "outside") {
            "هشدار: خودرو از محدوده‌ی تعریف‌شده خارج شد."
        } else {
            "خودرو به محدوده‌ی تعریف‌شده بازگشت."
        }
        broadcastToNumbers(context, settings.getAllowedNumbersList(), message)
    }

    private fun checkBatteryLevel(context: Context) {
        val settings = SettingsRepository(context)
        if (!settings.isBatteryAlertEnabled()) return

        val percent = BatteryHelper.getBatteryPercent(context)
        if (percent < 0) return // unknown right now, skip this tick

        val threshold = settings.getBatteryAlertThreshold()
        val newState = if (percent <= threshold) "low" else "normal"
        val previousState = settings.getBatteryAlertState()
        settings.saveBatteryAlertState(newState)

        if (previousState == null || previousState == newState) return // no real transition yet

        val message = if (newState == "low") {
            "هشدار: باتری گوشیِ ردیاب کم است ($percent٪)."
        } else {
            "باتری گوشیِ ردیاب دوباره در وضعیت عادی است ($percent٪)."
        }
        broadcastToNumbers(context, settings.getAllowedNumbersList(), message)
    }

    private fun broadcastToNumbers(context: Context, numbers: List<String>, message: String) {
        numbers.forEach { phone ->
            val data = Data.Builder()
                .putString(ReplyTextWorker.KEY_PHONE, phone)
                .putString(ReplyTextWorker.KEY_TEXT, message)
                .build()

            val request = OneTimeWorkRequestBuilder<ReplyTextWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
