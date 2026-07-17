package ir.lbo.locationsms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Local, on-device settings for the Tracker role. Reached from the main
 * TrackerActivity screen via the "تنظیمات" button — all fields are
 * pre-filled from whatever is already saved in SettingsRepository.
 */
class TrackerSettingsActivity : LockProtectedActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var phoneInput: EditText
    private lateinit var intervalInput: EditText
    private lateinit var allowedNumbersInput: EditText
    private lateinit var logIntervalInput: EditText
    private lateinit var minLogDistanceInput: EditText
    private lateinit var recipientEmailInput: EditText
    private lateinit var senderEmailInput: EditText
    private lateinit var senderEmailPasswordInput: EditText
    private lateinit var smtpHostInput: EditText
    private lateinit var smtpPortInput: EditText
    private lateinit var autoSendSwitch: Switch
    private lateinit var movementAlertSwitch: Switch
    private lateinit var batteryAlertSwitch: Switch
    private lateinit var batteryAlertThresholdInput: EditText
    private lateinit var geofenceSwitch: Switch
    private lateinit var geofenceRadiusInput: EditText
    private lateinit var geofenceCenterText: TextView
    private lateinit var statusText: TextView

    private val basePermissions: Array<String> by lazy {
        val list = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                requestBackgroundLocationIfNeeded()
            } else {
                statusText.text = "برخی مجوزها داده نشد. بدون آن‌ها برنامه به‌درستی کار نخواهد کرد."
            }
        }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Whether granted or not, proceed — foreground-only will still work
            // for the sendloc reply while the app is running.
            finishSetup()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker_settings)

        settings = SettingsRepository(this)
        phoneInput = findViewById(R.id.phoneInput)
        intervalInput = findViewById(R.id.intervalInput)
        allowedNumbersInput = findViewById(R.id.allowedNumbersInput)
        logIntervalInput = findViewById(R.id.logIntervalInput)
        minLogDistanceInput = findViewById(R.id.minLogDistanceInput)
        recipientEmailInput = findViewById(R.id.recipientEmailInput)
        senderEmailInput = findViewById(R.id.senderEmailInput)
        senderEmailPasswordInput = findViewById(R.id.senderEmailPasswordInput)
        smtpHostInput = findViewById(R.id.smtpHostInput)
        smtpPortInput = findViewById(R.id.smtpPortInput)
        autoSendSwitch = findViewById(R.id.autoSendSwitch)
        movementAlertSwitch = findViewById(R.id.movementAlertSwitch)
        batteryAlertSwitch = findViewById(R.id.batteryAlertSwitch)
        batteryAlertThresholdInput = findViewById(R.id.batteryAlertThresholdInput)
        geofenceSwitch = findViewById(R.id.geofenceSwitch)
        geofenceRadiusInput = findViewById(R.id.geofenceRadiusInput)
        geofenceCenterText = findViewById(R.id.geofenceCenterText)
        statusText = findViewById(R.id.statusText)

        phoneInput.setText(settings.getPhoneNumbersRaw())
        intervalInput.setText(settings.getIntervalMinutes().toString())
        allowedNumbersInput.setText(settings.getAllowedNumbersRaw())
        logIntervalInput.setText(settings.getLogIntervalMinutes().toString())
        minLogDistanceInput.setText(settings.getMinLogDistanceMeters().toString())
        recipientEmailInput.setText(settings.getRecipientEmail() ?: "")
        senderEmailInput.setText(settings.getSenderEmail() ?: "")
        senderEmailPasswordInput.setText(settings.getSenderEmailPassword() ?: "")
        smtpHostInput.setText(settings.getSmtpHost())
        smtpPortInput.setText(settings.getSmtpPort())
        autoSendSwitch.isChecked = settings.isAutoSendEnabled()
        movementAlertSwitch.isChecked = settings.isMovementAlertEnabled()
        batteryAlertSwitch.isChecked = settings.isBatteryAlertEnabled()
        batteryAlertThresholdInput.setText(settings.getBatteryAlertThreshold().toString())
        geofenceSwitch.isChecked = settings.isGeofenceEnabled()
        geofenceRadiusInput.setText(settings.getGeofenceRadiusMeters().toString())
        updateGeofenceCenterText()

        findViewById<Button>(R.id.saveButton).setOnClickListener { onSaveClicked() }
        findViewById<Button>(R.id.setGeofenceCenterButton).setOnClickListener {
            onSetGeofenceCenterClicked()
        }
    }

    private fun updateGeofenceCenterText() {
        val center = settings.getGeofenceCenter()
        geofenceCenterText.text = if (center != null) {
            "مرکز فعلی: ${center.first}, ${center.second}"
        } else {
            "مرکزی تنظیم نشده — روی دکمه‌ی بالا بزنید تا موقعیت فعلی به‌عنوان مرکز ثبت شود"
        }
    }

    private fun onSetGeofenceCenterClicked() {
        if (!hasAllBasePermissions()) {
            Toast.makeText(this, "ابتدا مجوز موقعیت مکانی را بدهید و دوباره تلاش کنید", Toast.LENGTH_SHORT).show()
            permissionLauncher.launch(basePermissions)
            return
        }

        Toast.makeText(this, "در حال دریافت موقعیت فعلی...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val location = LocationHelper.getCurrentLocation(this@TrackerActivity)
            if (location == null) {
                Toast.makeText(this@TrackerActivity, "دریافت موقعیت فعلی ممکن نشد", Toast.LENGTH_SHORT).show()
                return@launch
            }
            settings.saveGeofenceCenter(location.latitude, location.longitude)
            settings.saveGeofenceState("inside") // assume we're starting inside our own defined center
            updateGeofenceCenterText()
            Toast.makeText(this@TrackerActivity, "مرکز حصار جغرافیایی ثبت شد", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onSaveClicked() {
        val phonesRaw = phoneInput.text.toString().trim()
        val intervalText = intervalInput.text.toString().trim()
        val allowedRaw = allowedNumbersInput.text.toString().trim()
        val logIntervalText = logIntervalInput.text.toString().trim()
        val minLogDistanceText = minLogDistanceInput.text.toString().trim()
        val recipientEmail = recipientEmailInput.text.toString().trim()
        val senderEmail = senderEmailInput.text.toString().trim()
        val senderEmailPassword = senderEmailPasswordInput.text.toString().trim()
        val smtpHost = smtpHostInput.text.toString().trim().ifEmpty { "smtp.gmail.com" }
        val smtpPort = smtpPortInput.text.toString().trim().ifEmpty { "587" }
        val geofenceRadiusText = geofenceRadiusInput.text.toString().trim()
        val batteryThresholdText = batteryAlertThresholdInput.text.toString().trim()

        val phonesList = phonesRaw
            .split(",", "،", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (autoSendSwitch.isChecked && phonesList.isEmpty()) {
            Toast.makeText(this, "برای ارسال خودکار، حداقل یک شماره تلفن وارد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        if (movementAlertSwitch.isChecked && phonesList.isEmpty()) {
            Toast.makeText(this, "برای هشدار حرکت، حداقل یک شماره تلفن وارد کنید", Toast.LENGTH_SHORT).show()
            return
        }

        if (geofenceSwitch.isChecked && settings.getGeofenceCenter() == null) {
            Toast.makeText(this, "برای حصار جغرافیایی، ابتدا مرکز را با دکمه‌ی بالا ثبت کنید", Toast.LENGTH_LONG).show()
            return
        }

        val interval = intervalText.toLongOrNull() ?: 15L
        val safeInterval = if (interval < 15) 15L else interval

        val logInterval = logIntervalText.toLongOrNull() ?: 60L
        val safeLogInterval = if (logInterval < 15) 15L else logInterval

        val minLogDistance = minLogDistanceText.toLongOrNull() ?: 0L
        val safeMinLogDistance = if (minLogDistance < 0) 0L else minLogDistance

        val geofenceRadius = geofenceRadiusText.toLongOrNull() ?: 500L
        val safeGeofenceRadius = if (geofenceRadius < 50) 50L else geofenceRadius

        val batteryThreshold = batteryThresholdText.toLongOrNull() ?: 15L
        val safeBatteryThreshold = batteryThreshold.coerceIn(1L, 90L)

        if (interval < 15 || logInterval < 15) {
            Toast.makeText(this, "حداقل بازه مجاز ۱۵ دقیقه است", Toast.LENGTH_SHORT).show()
        }

        settings.savePhoneNumbersRaw(phonesRaw)
        settings.saveIntervalMinutes(safeInterval)
        settings.saveAllowedNumbersRaw(allowedRaw)
        settings.saveAutoSendEnabled(autoSendSwitch.isChecked)
        settings.saveMovementAlertEnabled(movementAlertSwitch.isChecked)
        settings.saveLogIntervalMinutes(safeLogInterval)
        settings.saveMinLogDistanceMeters(safeMinLogDistance)
        settings.saveRecipientEmail(recipientEmail)
        settings.saveSenderEmail(senderEmail)
        settings.saveSenderEmailPassword(senderEmailPassword)
        settings.saveSmtpHost(smtpHost)
        settings.saveSmtpPort(smtpPort)
        settings.saveGeofenceEnabled(geofenceSwitch.isChecked)
        settings.saveGeofenceRadiusMeters(safeGeofenceRadius)
        settings.saveBatteryAlertEnabled(batteryAlertSwitch.isChecked)
        settings.saveBatteryAlertThreshold(safeBatteryThreshold)

        batteryAlertThresholdInput.setText(safeBatteryThreshold.toString())
        intervalInput.setText(safeInterval.toString())
        logIntervalInput.setText(safeLogInterval.toString())
        minLogDistanceInput.setText(safeMinLogDistance.toString())
        geofenceRadiusInput.setText(safeGeofenceRadius.toString())

        if (settings.getAllowedNumbersList().isEmpty()) {
            Toast.makeText(
                this,
                "توجه: چون هیچ شماره مجازی ثبت نکردید، دستورات پیامکی (sendloc, autosend, sendlog, dellog, ping) و هشدارهای SIM/Geofence ارسال نمی‌شوند",
                Toast.LENGTH_LONG
            ).show()
        }

        if (!hasAllBasePermissions()) {
            permissionLauncher.launch(basePermissions)
        } else {
            requestBackgroundLocationIfNeeded()
        }
    }

    private fun hasAllBasePermissions(): Boolean =
        basePermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                return
            }
        }
        finishSetup()
    }

    private fun finishSetup() {
        // The location-log timer always runs, independent of the SMS
        // auto-send switch. It also handles SIM-change detection and
        // geofence checks on every tick.
        LogWorkScheduler.schedule(this, settings.getLogIntervalMinutes())

        if (autoSendSwitch.isChecked) {
            WorkScheduler.schedule(this, settings.getIntervalMinutes())
            statusText.text =
                "تنظیمات ذخیره شد. ارسال خودکار پیامکی هر ${settings.getIntervalMinutes()} دقیقه فعال است. " +
                "ذخیره در فایل هر ${settings.getLogIntervalMinutes()} دقیقه ادامه دارد."
        } else {
            WorkScheduler.cancel(this)
            statusText.text =
                "تنظیمات ذخیره شد. ارسال خودکار پیامکی غیرفعال است، اما ذخیره در فایل هر ${settings.getLogIntervalMinutes()} دقیقه ادامه دارد.\n" +
                "دستورات: 'sendloc'، 'sendlog'، 'dellog'، 'ping'."
        }
    }
}
