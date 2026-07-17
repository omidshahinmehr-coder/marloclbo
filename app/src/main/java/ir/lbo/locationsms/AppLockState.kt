package ir.lbo.locationsms

/**
 * Simple in-memory lock flag. It starts false and only becomes true after a
 * successful login. It resets back to false whenever the app's process is
 * actually restarted (fully closed/killed and reopened, device reboot,
 * etc.) — that's the ONLY time re-login is required. Navigating between
 * the app's own screens or briefly minimizing does not touch this flag.
 */
object AppLockState {
    @Volatile
    var unlocked: Boolean = false
}
