package ir.lbo.locationsms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Main Viewer dashboard: shows the received-message history and gives
 * direct access to every tracker command/setting. All commands are sent
 * straight from here; the "تنظیمات Tracker" button opens
 * ViewerTrackerSettingsActivity for the fuller remote-configuration form.
 */
class ViewerActivity : LockProtectedActivity() {

    private lateinit var historyListView: ListView
    private lateinit var historyAdapter: ViewerHistoryAdapter
    private lateinit var settings: SettingsRepository

    private val refreshListener: () -> Unit = { runOnUiThread { refreshAll() } }

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
            if (!results.values.all { it }) {
                Toast.makeText(
                    this,
                    "بدون مجوز پیامک، ارسال و دریافت فرامین ممکن نیست",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        settings = SettingsRepository(this)

        historyListView = findViewById(R.id.historyListView)
        historyAdapter = ViewerHistoryAdapter(this, ViewerHistoryStore.getAll(this))
        historyListView.adapter = historyAdapter
        historyListView.emptyView = findViewById<TextView>(R.id.emptyHistoryText)

        findViewById<Button>(R.id.clearHistoryButton).setOnClickListener { onClearHistoryClicked() }
        findViewById<Button>(R.id.sendlocButton).setOnClickListener { sendCommand("sendloc") }
        findViewById<Button>(R.id.sendlogButton).setOnClickListener { sendCommand("sendlog") }
        findViewById<Button>(R.id.dellogButton).setOnClickListener { sendCommand("dellog") }
        findViewById<Button>(R.id.pingButton).setOnClickListener { sendCommand("ping") }
        findViewById<Button>(R.id.showMapButton).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        findViewById<Button>(R.id.trackerSettingsButton).setOnClickListener {
            startActivity(Intent(this, ViewerTrackerSettingsActivity::class.java))
        }

        if (!hasAllPermissions()) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
        NewMessageNotifier.addListener(refreshListener)
    }

    override fun onPause() {
        super.onPause()
        NewMessageNotifier.removeListener(refreshListener)
    }

    private fun refreshAll() {
        historyAdapter.updateItems(ViewerHistoryStore.getAll(this))
    }

    private fun onClearHistoryClicked() {
        ViewerHistoryStore.clear(this)
        refreshAll()
        Toast.makeText(this, "تاریخچه پاک شد", Toast.LENGTH_SHORT).show()
    }

    private fun sendCommand(command: String) {
        val phone = settings.getTrackerViewerPhone()
        if (phone.isNullOrBlank()) {
            Toast.makeText(
                this,
                "ابتدا از «تنظیمات Tracker» شماره گوشی ردیاب را ذخیره کنید",
                Toast.LENGTH_LONG
            ).show()
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
