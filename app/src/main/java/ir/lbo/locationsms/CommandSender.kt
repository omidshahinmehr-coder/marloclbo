package ir.lbo.locationsms

import android.content.Context
import android.telephony.SmsManager

object CommandSender {
    fun send(context: Context, phone: String, command: String) {
        val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        } ?: return

        val parts = smsManager.divideMessage(command)
        smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
    }
}
