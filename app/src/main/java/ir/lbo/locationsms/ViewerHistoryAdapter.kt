package ir.lbo.locationsms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViewerHistoryAdapter(context: Context, private var items: List<ViewerHistoryEntry>) :
    // Wrap in ArrayList: Kotlin's emptyList() is immutable, and ArrayAdapter's
    // clear() calls .clear() on whatever list is passed in — using it directly
    // here would crash the first time the list is refreshed from empty.
    ArrayAdapter<ViewerHistoryEntry>(context, R.layout.item_history, ArrayList(items)) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun updateItems(newItems: List<ViewerHistoryEntry>) {
        items = newItems
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_history, parent, false)

        val entry = items[position]

        view.findViewById<TextView>(R.id.itemTimestamp).text =
            dateFormat.format(Date(entry.timestamp))

        view.findViewById<TextView>(R.id.itemText).text = entry.rawText

        val mapButton = view.findViewById<Button>(R.id.itemMapButton)
        if (entry.latitude != null && entry.longitude != null) {
            mapButton.visibility = View.VISIBLE
            mapButton.setOnClickListener {
                val uri = Uri.parse(
                    "geo:${entry.latitude},${entry.longitude}?q=${entry.latitude},${entry.longitude}"
                )
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        } else {
            mapButton.visibility = View.GONE
        }

        return view
    }
}
