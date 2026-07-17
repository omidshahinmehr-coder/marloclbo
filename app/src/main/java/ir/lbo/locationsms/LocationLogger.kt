package ir.lbo.locationsms

import android.content.Context
import android.location.Location
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocationLogger {

    private const val FILE_NAME = "location_log.csv"
    private const val ARCHIVE_PREFIX = "location_log_archive_"

    // Rotation period: roughly one month, to keep the active file from growing forever.
    private const val ROTATION_PERIOD_MILLIS = 30L * 24 * 60 * 60 * 1000

    /**
     * Appends one row (timestamp, latitude, longitude) to the CSV log file.
     * The file lives in the app's external files directory so it can be
     * pulled off the device with a file manager or `adb pull` without
     * needing root, and is automatically deleted if the app is uninstalled.
     *
     * Automatically rotates (archives) the current file first if it has
     * been open for longer than the rotation period.
     */
    fun appendEntry(context: Context, location: Location) {
        rotateIfNeeded(context)

        val file = getLogFile(context)
        val isNewFile = !file.exists()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val line = buildString {
            if (isNewFile) {
                append("timestamp,latitude,longitude\n")
            }
            append("$timestamp,${location.latitude},${location.longitude}\n")
        }
        file.appendText(line)

        val settings = SettingsRepository(context)
        if (settings.getLogStartTime() == 0L) {
            settings.saveLogStartTime(System.currentTimeMillis())
        }
    }

    private fun rotateIfNeeded(context: Context) {
        val settings = SettingsRepository(context)
        val startTime = settings.getLogStartTime()
        if (startTime == 0L) return // no active file yet, nothing to rotate

        val now = System.currentTimeMillis()
        if (now - startTime < ROTATION_PERIOD_MILLIS) return

        val currentFile = getLogFile(context)
        if (currentFile.exists()) {
            val suffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val archiveFile = File(currentFile.parentFile, "$ARCHIVE_PREFIX$suffix.csv")
            currentFile.renameTo(archiveFile)
        }

        // Reset so the next appendEntry() call starts counting a fresh period.
        settings.saveLogStartTime(0L)
    }

    fun getLogFile(context: Context): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, FILE_NAME)
    }

    fun getArchivedLogFiles(context: Context): List<File> {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return dir?.listFiles { f -> f.isFile && f.name.startsWith(ARCHIVE_PREFIX) }
            ?.toList()
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    /** Deletes archived (rotated) log files. Does not touch the current active file. */
    fun deleteArchivedLogFiles(context: Context): Int {
        var count = 0
        getArchivedLogFiles(context).forEach { file ->
            if (file.delete()) count++
        }
        return count
    }
}
