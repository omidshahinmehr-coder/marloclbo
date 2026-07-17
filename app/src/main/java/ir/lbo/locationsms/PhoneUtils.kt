package ir.lbo.locationsms

object PhoneUtils {

    /**
     * Normalizes a phone number for comparison by keeping only digits and
     * taking the last 10 digits. This makes "09121234567", "9121234567",
     * and "+989121234567" all compare as equal.
     */
    fun normalize(number: String): String {
        val digits = number.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun isAllowed(sender: String, allowedList: List<String>): Boolean {
        if (allowedList.isEmpty()) return false
        val normalizedSender = normalize(sender)
        if (normalizedSender.isEmpty()) return false
        return allowedList.any { normalize(it) == normalizedSender }
    }
}
