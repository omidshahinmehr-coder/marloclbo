package ir.lbo.locationsms

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

/**
 * Any screen reachable only after a successful login should extend this
 * instead of AppCompatActivity directly. It sends the user back to
 * LoginActivity only if AppLockState.unlocked is false — which happens
 * when the app process itself is freshly started (killed/closed and
 * reopened), since AppLockState is a plain in-memory flag. Navigating
 * between the app's own screens, briefly minimizing, or a system dialog
 * (permission prompt, biometric prompt) appearing on top does NOT clear
 * this flag, so the user is not asked to log in again in those cases —
 * only when the app was actually closed and relaunched.
 */
abstract class LockProtectedActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        if (!AppLockState.unlocked) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
