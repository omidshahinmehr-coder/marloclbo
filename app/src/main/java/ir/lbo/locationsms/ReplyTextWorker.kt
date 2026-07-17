package ir.lbo.locationsms

import android.content.Context
import android.telephony.SmsManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReplyTextWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_PHONE = "phone"
        const val KEY_TEXT = "text"
    }

    override suspend fun doWork(): Result {
        val phone = inputData.getString(KEY_PHONE) ?: return Result.failure()
        val text = inputData.getString(KEY_TEXT) ?: return Result.failure()

        val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
            applicationContext.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        } ?: return Result.failure()

        val parts = smsManager.divideMessage(text)
        smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
        return Result.success()
    }
}
