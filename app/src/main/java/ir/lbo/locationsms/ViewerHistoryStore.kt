package ir.lbo.locationsms

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ViewerHistoryEntry(
    val timestamp: Long,
    val rawText: String,
    val latitude: Double?,
    val longitude: Double?
)

object ViewerHistoryStore {
    private const val KEY_HISTORY = "viewer_history_entries"
    private const val MAX_ENTRIES = 300

    fun addEntry(context: Context, entry: ViewerHistoryEntry) {
        val list = getAll(context).toMutableList()
        list.add(0, entry) // newest first
        if (list.size > MAX_ENTRIES) {
            list.subList(MAX_ENTRIES, list.size).clear()
        }
        saveAll(context, list)
    }

    fun getAll(context: Context): List<ViewerHistoryEntry> {
        val prefs = context.getSharedPreferences("location_sms_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ViewerHistoryEntry(
                    timestamp = obj.getLong("timestamp"),
                    rawText = obj.getString("rawText"),
                    latitude = if (obj.has("lat")) obj.getDouble("lat") else null,
                    longitude = if (obj.has("lng")) obj.getDouble("lng") else null
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences("location_sms_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HISTORY)
            .apply()
    }

    private fun saveAll(context: Context, list: List<ViewerHistoryEntry>) {
        val array = JSONArray()
        list.forEach { entry ->
            val obj = JSONObject()
            obj.put("timestamp", entry.timestamp)
            obj.put("rawText", entry.rawText)
            entry.latitude?.let { obj.put("lat", it) }
            entry.longitude?.let { obj.put("lng", it) }
            array.put(obj)
        }
        context.getSharedPreferences("location_sms_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, array.toString())
            .apply()
    }
}
