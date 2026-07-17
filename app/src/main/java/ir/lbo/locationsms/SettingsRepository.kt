package ir.lbo.locationsms

import android.content.Context
import android.util.Base64

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("location_sms_prefs", Context.MODE_PRIVATE)

    fun savePhoneNumbersRaw(raw: String) {
        prefs.edit().putString(KEY_PHONE, raw).apply()
    }

    fun getPhoneNumbersRaw(): String = prefs.getString(KEY_PHONE, "") ?: ""

    fun getPhoneNumbersList(): List<String> =
        getPhoneNumbersRaw()
            .split(",", "،", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun saveIntervalMinutes(minutes: Long) {
        prefs.edit().putLong(KEY_INTERVAL, minutes).apply()
    }

    fun getIntervalMinutes(): Long = prefs.getLong(KEY_INTERVAL, 60L)

    fun saveAutoSendEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isAutoSendEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    // --- Allowed sender numbers (whitelist for "sendloc") ---

    fun saveAllowedNumbersRaw(raw: String) {
        prefs.edit().putString(KEY_ALLOWED, raw).apply()
    }

    fun getAllowedNumbersRaw(): String = prefs.getString(KEY_ALLOWED, "") ?: ""

    fun getAllowedNumbersList(): List<String> =
        getAllowedNumbersRaw()
            .split(",", "،", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    // --- App-open password ---

    fun isPasswordSet(): Boolean = prefs.contains(KEY_PASS_HASH)

    fun setPassword(password: String) {
        val salt = PasswordUtils.generateSalt()
        val hash = PasswordUtils.hash(password, salt)
        prefs.edit()
            .putString(KEY_PASS_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASS_HASH, hash)
            .apply()
    }

    fun verifyPassword(password: String): Boolean {
        val saltB64 = prefs.getString(KEY_PASS_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PASS_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val hash = PasswordUtils.hash(password, salt)
        return hash == storedHash
    }

    // --- Biometric login ---

    fun saveBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    // --- Viewer mode: phone number of the tracker device to control ---

    fun saveTrackerViewerPhone(phone: String) {
        prefs.edit().putString(KEY_TRACKER_VIEWER_PHONE, phone).apply()
    }

    fun getTrackerViewerPhone(): String? = prefs.getString(KEY_TRACKER_VIEWER_PHONE, null)

    // --- Independent location-log timer ---

    fun saveLogIntervalMinutes(minutes: Long) {
        prefs.edit().putLong(KEY_LOG_INTERVAL, minutes).apply()
    }

    fun getLogIntervalMinutes(): Long = prefs.getLong(KEY_LOG_INTERVAL, 60L)

    /** 0 means "no active log file yet" (used to detect when to start counting for rotation). */
    fun saveLogStartTime(millis: Long) {
        prefs.edit().putLong(KEY_LOG_START, millis).apply()
    }

    fun getLogStartTime(): Long = prefs.getLong(KEY_LOG_START, 0L)

    // --- Email / SMTP settings (used by the "sendlog" command) ---

    fun saveRecipientEmail(email: String) {
        prefs.edit().putString(KEY_RECIPIENT_EMAIL, email).apply()
    }

    fun getRecipientEmail(): String? = prefs.getString(KEY_RECIPIENT_EMAIL, null)

    fun saveSenderEmail(email: String) {
        prefs.edit().putString(KEY_SENDER_EMAIL, email).apply()
    }

    fun getSenderEmail(): String? = prefs.getString(KEY_SENDER_EMAIL, null)

    fun saveSenderEmailPassword(password: String) {
        prefs.edit().putString(KEY_SENDER_EMAIL_PASSWORD, password).apply()
    }

    fun getSenderEmailPassword(): String? = prefs.getString(KEY_SENDER_EMAIL_PASSWORD, null)

    fun saveSmtpHost(host: String) {
        prefs.edit().putString(KEY_SMTP_HOST, host).apply()
    }

    fun getSmtpHost(): String = prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com") ?: "smtp.gmail.com"

    fun saveSmtpPort(port: String) {
        prefs.edit().putString(KEY_SMTP_PORT, port).apply()
    }

    fun getSmtpPort(): String = prefs.getString(KEY_SMTP_PORT, "587") ?: "587"

    // --- SIM change detection ---

    fun saveSimFingerprint(fingerprint: String) {
        prefs.edit().putString(KEY_SIM_FINGERPRINT, fingerprint).apply()
    }

    fun getSimFingerprint(): String? = prefs.getString(KEY_SIM_FINGERPRINT, null)

    // --- Distance-based logging (0 = disabled, log on every timer tick) ---

    fun saveMinLogDistanceMeters(meters: Long) {
        prefs.edit().putLong(KEY_MIN_LOG_DISTANCE, meters).apply()
    }

    fun getMinLogDistanceMeters(): Long = prefs.getLong(KEY_MIN_LOG_DISTANCE, 0L)

    fun saveMovementAlertEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MOVEMENT_ALERT_ENABLED, enabled).apply()
    }

    fun isMovementAlertEnabled(): Boolean = prefs.getBoolean(KEY_MOVEMENT_ALERT_ENABLED, false)

    fun saveLastLoggedLocation(lat: Double, lng: Double) {
        prefs.edit()
            .putFloat(KEY_LAST_LOG_LAT, lat.toFloat())
            .putFloat(KEY_LAST_LOG_LNG, lng.toFloat())
            .apply()
    }

    /** Returns Pair(lat, lng) or null if nothing logged yet. */
    fun getLastLoggedLocation(): Pair<Double, Double>? {
        if (!prefs.contains(KEY_LAST_LOG_LAT) || !prefs.contains(KEY_LAST_LOG_LNG)) return null
        val lat = prefs.getFloat(KEY_LAST_LOG_LAT, 0f).toDouble()
        val lng = prefs.getFloat(KEY_LAST_LOG_LNG, 0f).toDouble()
        return lat to lng
    }

    // --- Geofence ---

    fun saveGeofenceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GEOFENCE_ENABLED, enabled).apply()
    }

    fun isGeofenceEnabled(): Boolean = prefs.getBoolean(KEY_GEOFENCE_ENABLED, false)

    fun saveGeofenceCenter(lat: Double, lng: Double) {
        prefs.edit()
            .putFloat(KEY_GEOFENCE_LAT, lat.toFloat())
            .putFloat(KEY_GEOFENCE_LNG, lng.toFloat())
            .apply()
    }

    /** Returns Pair(lat, lng) or null if no center has been set yet. */
    fun getGeofenceCenter(): Pair<Double, Double>? {
        if (!prefs.contains(KEY_GEOFENCE_LAT) || !prefs.contains(KEY_GEOFENCE_LNG)) return null
        val lat = prefs.getFloat(KEY_GEOFENCE_LAT, 0f).toDouble()
        val lng = prefs.getFloat(KEY_GEOFENCE_LNG, 0f).toDouble()
        return lat to lng
    }

    fun saveGeofenceRadiusMeters(meters: Long) {
        prefs.edit().putLong(KEY_GEOFENCE_RADIUS, meters).apply()
    }

    fun getGeofenceRadiusMeters(): Long = prefs.getLong(KEY_GEOFENCE_RADIUS, 500L)

    /** "inside", "outside", or null if not known yet. */
    fun saveGeofenceState(state: String) {
        prefs.edit().putString(KEY_GEOFENCE_STATE, state).apply()
    }

    fun getGeofenceState(): String? = prefs.getString(KEY_GEOFENCE_STATE, null)

    // --- Low battery alert ---

    fun saveBatteryAlertEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BATTERY_ALERT_ENABLED, enabled).apply()
    }

    fun isBatteryAlertEnabled(): Boolean = prefs.getBoolean(KEY_BATTERY_ALERT_ENABLED, false)

    fun saveBatteryAlertThreshold(percent: Long) {
        prefs.edit().putLong(KEY_BATTERY_ALERT_THRESHOLD, percent).apply()
    }

    fun getBatteryAlertThreshold(): Long = prefs.getLong(KEY_BATTERY_ALERT_THRESHOLD, 15L)

    /** "low", "normal", or null if not known yet. */
    fun saveBatteryAlertState(state: String) {
        prefs.edit().putString(KEY_BATTERY_ALERT_STATE, state).apply()
    }

    fun getBatteryAlertState(): String? = prefs.getString(KEY_BATTERY_ALERT_STATE, null)

    // --- Remote (SMS-driven) settings, changeable from the Viewer's tracker-settings screen ---

    companion object {
        private const val KEY_PHONE = "phone_number"
        private const val KEY_INTERVAL = "interval_minutes"
        private const val KEY_ENABLED = "auto_send_enabled"
        private const val KEY_ALLOWED = "allowed_numbers"
        private const val KEY_PASS_SALT = "pass_salt"
        private const val KEY_PASS_HASH = "pass_hash"
        private const val KEY_LOG_INTERVAL = "log_interval_minutes"
        private const val KEY_LOG_START = "log_start_time"
        private const val KEY_RECIPIENT_EMAIL = "recipient_email"
        private const val KEY_SENDER_EMAIL = "sender_email"
        private const val KEY_SENDER_EMAIL_PASSWORD = "sender_email_password"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_TRACKER_VIEWER_PHONE = "tracker_viewer_phone"
        private const val KEY_SIM_FINGERPRINT = "sim_fingerprint"
        private const val KEY_MIN_LOG_DISTANCE = "min_log_distance_meters"
        private const val KEY_MOVEMENT_ALERT_ENABLED = "movement_alert_enabled"
        private const val KEY_LAST_LOG_LAT = "last_log_lat"
        private const val KEY_LAST_LOG_LNG = "last_log_lng"
        private const val KEY_GEOFENCE_ENABLED = "geofence_enabled"
        private const val KEY_GEOFENCE_LAT = "geofence_lat"
        private const val KEY_GEOFENCE_LNG = "geofence_lng"
        private const val KEY_GEOFENCE_RADIUS = "geofence_radius_meters"
        private const val KEY_GEOFENCE_STATE = "geofence_state"
        private const val KEY_BATTERY_ALERT_ENABLED = "battery_alert_enabled"
        private const val KEY_BATTERY_ALERT_THRESHOLD = "battery_alert_threshold"
        private const val KEY_BATTERY_ALERT_STATE = "battery_alert_state"
    }
}
