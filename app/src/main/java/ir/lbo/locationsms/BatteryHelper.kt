package ir.lbo.locationsms

import android.content.Context
import android.os.BatteryManager

object BatteryHelper {
    fun getBatteryPercent(context: Context): Int {
        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
