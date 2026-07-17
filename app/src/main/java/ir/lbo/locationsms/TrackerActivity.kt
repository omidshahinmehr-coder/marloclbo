package ir.lbo.locationsms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Main Tracker dashboard, split in two parts:
 *  1) A log-file viewer with a "Browse" button to pick any CSV log file
 *     (the current active log, or an archived one) and show its content.
 *  2) A button that opens TrackerSettingsActivity, where all of the
 *     tracker's current settings live and can be changed.
 */
class TrackerActivity : LockProtectedActivity() {

    private lateinit var logFileNameText: TextView
    private lateinit var logFileContentText: TextView

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) loadLogFile(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker)

        logFileNameText = findViewById(R.id.logFileNameText)
        logFileContentText = findViewById(R.id.logFileContentText)

        findViewById<Button>(R.id.browseLogButton).setOnClickListener {
            openDocumentLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "application/octet-stream"))
        }

        findViewById<Button>(R.id.openTrackerSettingsButton).setOnClickListener {
            startActivity(Intent(this, TrackerSettingsActivity::class.java))
        }

        loadDefaultActiveLog()
    }

    private fun loadDefaultActiveLog() {
        val file = LocationLogger.getLogFile(this)
        if (!file.exists()) {
            logFileNameText.text = "هنوز فایل لاگی ایجاد نشده است."
            logFileContentText.text = "—"
            return
        }
        try {
            logFileNameText.text = "فایل فعال: ${file.name}"
            logFileContentText.text = file.readText().ifBlank { "—" }
        } catch (e: Exception) {
            logFileNameText.text = "خطا در خواندن فایل لاگ فعال."
            logFileContentText.text = "—"
        }
    }

    private fun loadLogFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val text = stream.bufferedReader().readText()
                logFileNameText.text = "فایل انتخاب‌شده: ${queryDisplayName(uri) ?: uri.lastPathSegment ?: uri.toString()}"
                logFileContentText.text = text.ifBlank { "—" }
            } ?: run {
                Toast.makeText(this, "امکان باز کردن فایل نبود", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در خواندن فایل انتخاب‌شده", Toast.LENGTH_SHORT).show()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
