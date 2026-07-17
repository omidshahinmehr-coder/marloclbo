package ir.lbo.locationsms

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the location-logging worker. This runs independently of the
 * SMS auto-send feature, on its own timer, and keeps running regardless
 * of whether SMS auto-send is enabled or disabled.
 */
object LogWorkScheduler {
    private const val WORK_NAME = "location_log_work"

    fun schedule(context: Context, intervalMinutes: Long) {
        val interval = if (intervalMinutes < 15) 15 else intervalMinutes

        val request = PeriodicWorkRequestBuilder<LocationLogWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
