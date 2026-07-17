package ir.lbo.locationsms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Lets the Viewer change every SMS-driven Tracker setting: autosend,
 * movement alert, geofence, low-battery alert, admin/autosend number
 * lists, and the log-save timer. Every action here composes and sends
 * one SMS command to the saved tracker phone number.
 */
class ViewerTrackerSettingsActivity : LockProtectedActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var trackerPhoneInput: EditText

    private val requiredPermissions: Array<String> by lazy {
        val list = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                Toast.makeText(this, "مجوزها با موفقیت داده شد", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "بدون مجوز پیامک، این حالت نمی‌تواند دستور بفرستد یا پاسخ دریافت کند",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer_tracker_settings)

        settings = SettingsRepository(this)
        trackerPhoneInput = findViewById(R.id.trackerPhoneInput)
        trackerPhoneInput.setText(settings.getTrackerViewerPhone() ?: "")

        findViewById<Button>(R.id.saveTrackerPhoneButton).setOnClickListener { onSaveTrackerPhone() }

        val autosendIntervalInput = findViewById<EditText>(R.id.autosendIntervalInput)
        findViewById<Button>(R.id.autosendOnButton).setOnClickListener {
            val minutes = autosendIntervalInput.text.toString().trim().toLongOrNull() ?: 15L
            val safe = if (minutes < 15) 15L else minutes
            autosendIntervalInput.setText(safe.toString())
            sendCommand("autosend on $safe")
        }
        findViewById<Button>(R.id.autosendOffButton).setOnClickListener { sendCommand("autosend off") }

        val moveAlertDistanceInput = findViewById<EditText>(R.id.moveAlertDistanceInput)
        findViewById<Button>(R.id.moveAlertOnButton).setOnClickListener {
            val distance = moveAlertDistanceInput.text.toString().trim().toLongOrNull() ?: 50L
            val safe = if (distance < 0) 0L else distance
            moveAlertDistanceInput.setText(safe.toString())
            sendCommand("MoveAlert $safe on")
        }
        findViewById<Button>(R.id.moveAlertOffButton).setOnClickListener { sendCommand("MoveAlert off") }

        val geofenceRadiusInput = findViewById<EditText>(R.id.geofenceRadiusInput)
        findViewById<Button>(R.id.geofenceOnButton).setOnClickListener {
            val radius = geofenceRadiusInput.text.toString().trim().toLongOrNull() ?: 500L
            val safe = if (radius < 50) 50L else radius
            geofenceRadiusInput.setText(safe.toString())
            sendCommand("Geofence on $safe")
        }
        findViewById<Button>(R.id.geofenceOffButton).setOnClickListener { sendCommand("Geofence off") }

        val lowBatteryPercentInput = findViewById<EditText>(R.id.lowBatteryPercentInput)
        findViewById<Button>(R.id.lowBatteryOnButton).setOnClickListener {
            val percent = lowBatteryPercentInput.text.toString().trim().toLongOrNull() ?: 15L
            val safe = percent.coerceIn(1L, 90L)
            lowBatteryPercentInput.setText(safe.toString())
            sendCommand("LowbatteryAlert $safe on")
        }
        findViewById<Button>(R.id.lowBatteryOffButton).setOnClickListener { sendCommand("LowbatteryAlert off") }

        val adminNumbersInput = findViewById<EditText>(R.id.adminNumbersInput)
        findViewById<Button>(R.id.setAdminNumbersButton).setOnClickListener {
            val numbers = adminNumbersInput.text.toString().trim()
            if (numbers.isEmpty()) {
                Toast.makeText(this, "حداقل یک شماره وارد کنید", Toast.LENGTH_SHORT).show()
            } else {
                sendCommand("SetAdminNumbers $numbers")
            }
        }

        val autosendNumbersInput = findViewById<EditText>(R.id.autosendNumbersInput)
        findViewById<Button>(R.id.setAutosendNumbersButton).setOnClickListener {
            val numbers = autosendNumbersInput.text.toString().trim()
            if (numbers.isEmpty()) {
                Toast.makeText(this, "حداقل یک شماره وارد کنید", Toast.LENGTH_SHORT).show()
            } else {
                sendCommand("SetAutosendNumbers $numbers")
            }
        }

        val logTimerInput = findViewById<EditText>(R.id.logTimerInput)
        findViewById<Button>(R.id.setLogTimerButton).setOnClickListener {
            val minutes = logTimerInput.text.toString().trim().toLongOrNull() ?: 15L
            val safe = if (minutes < 15) 15L else minutes
            logTimerInput.setText(safe.toString())
            sendCommand("SetLogTimer $safe")
        }

        if (!hasAllPermissions()) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun onSaveTrackerPhone() {
        val phone = trackerPhoneInput.text.toString().trim()
        if (phone.isEmpty()) {
            Toast.makeText(this, "شماره گوشی ردیاب را وارد کنید", Toast.LENGTH_SHORT).show()
            return
        }
        settings.saveTrackerViewerPhone(phone)
        Toast.makeText(this, "شماره ذخیره شد", Toast.LENGTH_SHORT).show()

        if (!hasAllPermissions()) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun sendCommand(command: String) {
        val phone = settings.getTrackerViewerPhone()
        if (phone.isNullOrBlank()) {
            Toast.makeText(this, "ابتدا شماره گوشی ردیاب را ذخیره کنید", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasAllPermissions()) {
            Toast.makeText(this, "ابتدا مجوز پیامک را بدهید", Toast.LENGTH_SHORT).show()
            permissionLauncher.launch(requiredPermissions)
            return
        }

        CommandSender.send(this, phone, command)
        Toast.makeText(this, "دستور «$command» ارسال شد", Toast.LENGTH_SHORT).show()
    }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
}
