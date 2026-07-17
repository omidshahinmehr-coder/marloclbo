package ir.lbo.locationsms

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val WORK_NAME = "auto_location_work"

    fun schedule(context: Context, intervalMinutes: Long) {
        // WorkManager enforces a 15-minute minimum for periodic work
        val interval = if (intervalMinutes < 15) 15 else intervalMinutes

        val request = PeriodicWorkRequestBuilder<AutoLocationWorker>(interval, TimeUnit.MINUTES)
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
