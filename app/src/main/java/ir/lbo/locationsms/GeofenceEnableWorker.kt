package ir.lbo.locationsms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * Handles the remote "Geofence on <dim>" command. If no geofence center
 * has been set yet (e.g. from the local Tracker settings screen), it
 * fetches the current location and uses it as the center, then enables
 * the geofence and confirms by SMS.
 */
class GeofenceEnableWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_PHONE = "phone"
    }

    override suspend fun doWork(): Result {
        val phone = inputData.getString(KEY_PHONE) ?: return Result.failure()
        val settings = SettingsRepository(applicationContext)

        if (settings.getGeofenceCenter() == null) {
            val location = LocationHelper.getCurrentLocation(applicationContext)
                ?: return Result.retry()
            settings.saveGeofenceCenter(location.latitude, location.longitude)
            settings.saveGeofenceState("inside")
        }

        settings.saveGeofenceEnabled(true)
        replyText(phone, "حصار جغرافیایی با شعاع ${settings.getGeofenceRadiusMeters()} متر فعال شد.")
        return Result.success()
    }

    private fun replyText(phone: String, text: String) {
        val data = Data.Builder()
            .putString(ReplyTextWorker.KEY_PHONE, phone)
            .putString(ReplyTextWorker.KEY_TEXT, text)
            .build()

        val request = OneTimeWorkRequestBuilder<ReplyTextWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)
    }
}
