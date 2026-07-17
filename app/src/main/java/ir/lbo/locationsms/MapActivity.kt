package ir.lbo.locationsms

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapActivity : LockProtectedActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val webView = findViewById<WebView>(R.id.mapWebView)
        webView.settings.javaScriptEnabled = true

        val pointsJson = buildPointsJson()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                view.evaluateJavascript("renderPoints('$pointsJson')", null)
            }
        }

        webView.loadUrl("file:///android_asset/map.html")
    }

    private fun buildPointsJson(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val entries = ViewerHistoryStore.getAll(this)
            .filter { it.latitude != null && it.longitude != null }
            .sortedBy { it.timestamp } // oldest first, so the route line draws in order

        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("lat", entry.latitude)
            obj.put("lng", entry.longitude)
            obj.put("label", dateFormat.format(Date(entry.timestamp)))
            array.put(obj)
        }

        // Escape single quotes since the JSON is embedded inside a single-quoted
        // JS string literal in evaluateJavascript().
        return array.toString().replace("'", "\\'")
    }
}
