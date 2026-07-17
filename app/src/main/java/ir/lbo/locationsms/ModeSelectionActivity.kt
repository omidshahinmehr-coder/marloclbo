package ir.lbo.locationsms

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class ModeSelectionActivity : LockProtectedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mode_selection)

        findViewById<Button>(R.id.trackerModeButton).setOnClickListener {
            startActivity(Intent(this, TrackerActivity::class.java))
        }

        findViewById<Button>(R.id.viewerModeButton).setOnClickListener {
            startActivity(Intent(this, ViewerActivity::class.java))
        }

        findViewById<Button>(R.id.securitySettingsButton).setOnClickListener {
            startActivity(Intent(this, SecuritySettingsActivity::class.java))
        }
    }
}
