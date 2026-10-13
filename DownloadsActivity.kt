package com.sgo.browser

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

class DownloadsActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout
    private val h = Handler(Looper.getMainLooper())
    private var sig = ""
    private val tick = object : Runnable { override fun run() { refresh(); h.postDelayed(this, 1000) } }

    override fun onCreate(s: Bundle?) {
        Store.applyTheme(this); super.onCreate(s)
        window.statusBarColor = col(R.color.bg); window.navigationBarColor = col(R.color.bg)
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(col(R.color.bg)); fitsSystemWindows = true }
        setContentView(root)
        WindowInsetsControllerCompat(window, root).apply { isAppearanceLightStatusBars = !night; isAppearanceLightNavigationBars = !night }
        root.addView(header("Downloads") { finish() })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(6), dp(14), dp(14)) }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    override fun onResume() { super.onResume(); sig = ""; h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

    private fun status(dm: DownloadManager, d: Dl): Pair<String, Boolean> {
        val f = File(d.path)
        dm.query(DownloadManager.Query().setFilterById(d.id)).use { c ->
            if (c.moveToFirst()) {
                val st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val got = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val tot = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                return when (st) {
                    DownloadManager.STATUS_SUCCESSFUL -> Util.size(if (tot > 0) tot else f.length()).ifEmpty { "Downloaded" } to true
                    DownloadManager.STATUS_FAILED -> "Failed" to false
                    DownloadManager.STATUS_PAUSED -> "Paused" to false
                    else -> (if (tot > 0) "${got * 100 / tot}%" else "Downloading…") to false
                }
            }
        }
        return if (f.exists()) Util.size(f.length()) to true else "Removed" to false
    }

    private fun refresh() {
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val ds = Store.downloads(this)
        val st = ds.map { status(dm, it) }
        val s = st.joinToString("|") { it.first + it.second } + ds.size
        if (s == sig) return
        sig = s
        list.removeAllViews()
        if (ds.isEmpty()) { list.addView(tv("No downloads yet", 14f, col(R.color.sub)).apply { gravity = Gravity.CENTER; setPadding(0, dp(80), 0, 0) }, LinearLayout.LayoutParams(-1, -2)); return }
        ds.forEachIndexed { i, d ->
            val (text, done) = st[i]
            val r = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = round(col(R.color.card), 12); setPadding(dp(16), dp(12), dp(10), dp(12)); elevation = dp(1).toFloat() }
            val c1 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            c1.addView(tv(d.name, 14f).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.MIDDLE })
            c1.addView(tv(text, 12f, col(R.color.sub)))
            r.addView(c1, LinearLayout.LayoutParams(0, -2, 1f))
            if (done) {
                val apk = d.name.endsWith(".apk", true)
                r.addView(tv(if (apk) "Install" else "Open", 12f, android.graphics.Color.WHITE).apply {
                    gravity = Gravity.CENTER; background = round(col(R.color.accent), 16); setPadding(dp(16), dp(7), dp(16), dp(7))
                    setOnClickListener { if (apk) Util.install(this@DownloadsActivity, File(d.path)) else Util.open(this@DownloadsActivity, File(d.path)) }
                })
            }
            r.addView(icon(R.drawable.ic_close, 18, col(R.color.sub)).apply {
                setPadding(dp(8), dp(8), dp(8), dp(8)); layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginStart = dp(6) }
                setOnClickListener {
                    AlertDialog.Builder(this@DownloadsActivity).setTitle("Delete ${d.name}?").setPositiveButton("Delete") { _, _ ->
                        dm.remove(d.id); File(d.path).delete(); Store.removeDownload(this@DownloadsActivity, d.id); sig = ""; refresh() }.setNegativeButton("Cancel", null).show()
                }
            })
            list.addView(r, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        }
    }
}
