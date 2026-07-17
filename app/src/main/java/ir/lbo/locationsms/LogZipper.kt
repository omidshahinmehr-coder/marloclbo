package ir.lbo.locationsms

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object LogZipper {

    /**
     * Zips the given files into a single archive inside the app's cache
     * directory and returns it, or null if there was nothing to zip or an
     * error occurred. The caller is responsible for deleting the returned
     * file once it's no longer needed (e.g. after the email is sent).
     */
    fun createZip(context: Context, files: List<File>): File? {
        val existingFiles = files.filter { it.exists() }
        if (existingFiles.isEmpty()) return null

        val suffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File(context.cacheDir, "location_logs_$suffix.zip")

        return try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                existingFiles.forEach { file ->
                    BufferedInputStream(FileInputStream(file)).use { input ->
                        zipOut.putNextEntry(ZipEntry(file.name))
                        input.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                }
            }
            zipFile
        } catch (e: Exception) {
            zipFile.delete()
            null
        }
    }
}
