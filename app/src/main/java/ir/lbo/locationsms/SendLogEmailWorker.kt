package ir.lbo.locationsms

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendLogEmailWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_REPLY_PHONE = "reply_phone"
    }

    override suspend fun doWork(): Result {
        val replyPhone = inputData.getString(KEY_REPLY_PHONE)

        // Safety net: on very old/unusual Android builds, a missing API can
        // throw a NoSuchMethodError (an Error, not an Exception) that would
        // otherwise silently kill this worker with no SMS reply at all —
        // exactly what happened on Android 5.1 with getActiveNetwork().
        // Catching Throwable here guarantees the user always gets *some*
        // reply instead of total silence.
        return try {
            doWorkInternal(replyPhone)
        } catch (t: Throwable) {
            replyPhone?.let {
                val shortError = (t.message ?: t.javaClass.simpleName).take(80)
                sendReply(applicationContext, it, "خطای غیرمنتظره در ارسال فایل لاگ: $shortError")
            }
            Result.success()
        }
    }

    private suspend fun doWorkInternal(replyPhone: String?): Result {
        if (!hasInternetConnection(applicationContext)) {
            replyPhone?.let {
                sendReply(applicationContext, it, "اتصال اینترنت برقرار نیست؛ ارسال فایل لاگ به ایمیل انجام نشد.")
            }
            return Result.success()
        }

        val settings = SettingsRepository(applicationContext)
        val senderEmail = settings.getSenderEmail()
        val senderPassword = settings.getSenderEmailPassword()
        val recipientEmail = settings.getRecipientEmail()
        val smtpHost = settings.getSmtpHost()
        val smtpPort = settings.getSmtpPort()

        if (senderEmail.isNullOrBlank() || senderPassword.isNullOrBlank() || recipientEmail.isNullOrBlank()) {
            replyPhone?.let {
                sendReply(applicationContext, it, "تنظیمات ایمیل کامل نیست؛ ابتدا از داخل اپ ایمیل‌ها را تنظیم کنید.")
            }
            return Result.success()
        }

        val files = mutableListOf<java.io.File>()
        val currentFile = LocationLogger.getLogFile(applicationContext)
        if (currentFile.exists()) files.add(currentFile)
        files.addAll(LocationLogger.getArchivedLogFiles(applicationContext))

        if (files.isEmpty()) {
            replyPhone?.let {
                sendReply(applicationContext, it, "فایل لاگی برای ارسال وجود ندارد.")
            }
            return Result.success()
        }

        val zipFile = withContext(Dispatchers.IO) {
            LogZipper.createZip(applicationContext, files)
        }

        if (zipFile == null) {
            replyPhone?.let {
                sendReply(applicationContext, it, "فشرده‌سازی فایل‌های لاگ با خطا مواجه شد.")
            }
            return Result.success()
        }

        val result = withContext(Dispatchers.IO) {
            EmailSender.sendLogEmail(
                smtpHost = smtpHost,
                smtpPort = smtpPort,
                senderEmail = senderEmail,
                senderPassword = senderPassword,
                recipientEmail = recipientEmail,
                subject = "My Car Tracker - فایل لاگ موقعیت مکانی",
                bodyText = "فایل لاگ موقعیت مکانی این خودرو به‌صورت فشرده (zip) پیوست شده است.",
                attachmentFiles = listOf(zipFile)
            )
        }

        zipFile.delete()

        replyPhone?.let {
            val message = if (result.success) {
                "فایل لاگ (فشرده) با موفقیت به ایمیل ارسال شد."
            } else {
                val shortError = result.errorMessage?.take(80) ?: "نامشخص"
                "ارسال فایل لاگ به ایمیل با خطا مواجه شد: $shortError"
            }
            sendReply(applicationContext, it, message)
        }

        return Result.success()
    }

    private fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            // getActiveNetwork()/getNetworkCapabilities() need API 23+.
            // Older devices (e.g. Android 5.x) must use the deprecated but
            // universally-available getActiveNetworkInfo() instead.
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected
        }
    }

    private fun sendReply(context: Context, phone: String, text: String) {
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
