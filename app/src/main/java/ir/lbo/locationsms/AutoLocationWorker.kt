package ir.lbo.locationsms

import android.content.Context
import android.telephony.SmsManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AutoLocationWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext)
        val phones = settings.getPhoneNumbersList()
        if (phones.isEmpty()) return Result.success()

        val location = LocationHelper.getCurrentLocation(applicationContext)
            ?: return Result.retry()

        val mapsUrl = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        val text = "Auto location: ${location.latitude}, ${location.longitude}\n$mapsUrl"

        phones.forEach { phone -> sendSms(phone, text) }
        return Result.success()
    }

    private fun sendSms(phone: String, text: String) {
        val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
            applicationContext.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        } ?: return

        val parts = smsManager.divideMessage(text)
        smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
    }
}
