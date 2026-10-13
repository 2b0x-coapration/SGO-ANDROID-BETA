package com.sgo.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var home: View
    private lateinit var searchBox: LinearLayout
    private lateinit var cards: LinearLayout
    private lateinit var input: EditText
    private lateinit var bottom: LinearLayout
    private lateinit var title: TextView
    private lateinit var bar: ProgressBar
    private var searching = false

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val id = i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val d = Store.downloads(c).firstOrNull { it.id == id } ?: return
            if (d.name.endsWith(".apk", true)) AlertDialog.Builder(this@MainActivity).setTitle("Install ${d.name}?")
                .setPositiveButton("Install") { _, _ -> Util.install(this@MainActivity, File(d.path)) }
                .setNegativeButton("Later", null).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(s: Bundle?) {
        Store.applyTheme(this); super.onCreate(s)
        window.statusBarColor = col(R.color.bg); window.navigationBarColor = col(R.color.card)
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(col(R.color.bg)); fitsSystemWindows = true }
        setContentView(root)
        WindowInsetsControllerCompat(window, root).apply { isAppearanceLightStatusBars = !night; isAppearanceLightNavigationBars = !night }

        val top = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(-1, dp(52)) }
        top.addView(icon(R.drawable.ic_s, 30, col(R.color.accent)), FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER_VERTICAL or Gravity.START).apply { marginStart = dp(14) })
        title = tv("SGO", 16f).apply { gravity = Gravity.CENTER; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        top.addView(title, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply { marginStart = dp(60); marginEnd = dp(60) })
        val menu = icon(R.drawable.ic_menu, 22)
        top.addView(menu, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER_VERTICAL or Gravity.END).apply { marginEnd = dp(16) })
        menu.setOnClickListener { v ->
            val m = PopupMenu(this, v); listOf("Home", "Reload", "Downloads", "Settings").forEach { m.menu.add(it) }
            m.setOnMenuItemClickListener {
                when (it.title.toString()) {
                    "Home" -> showHome()
                    "Reload" -> if (web.visibility == View.VISIBLE) web.reload()
                    "Downloads" -> startActivity(Intent(this, DownloadsActivity::class.java))
                    else -> startActivity(Intent(this, SettingsActivity::class.java))
                }; true }
            m.show()
        }
        root.addView(top)

        val content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val h = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        h.addView(tv("SGO", 46f, col(R.color.text)).apply { typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL); letterSpacing = 0.15f; gravity = Gravity.CENTER })
        val pill = LinearLayout(this).apply { gravity = Gravity.CENTER; background = round(col(R.color.pill), 17); setOnClickListener { openSearch() } }
        pill.addView(icon(R.drawable.ic_search, 15, col(R.color.sub)))
        pill.addView(tv("  Search", 12f, col(R.color.sub)))
        h.addView(pill, LinearLayout.LayoutParams(dp(200), dp(34)).apply { topMargin = dp(8) })
        home = h; content.addView(h, FrameLayout.LayoutParams(-1, -1))

        web = WebView(this).apply { visibility = View.GONE }
        content.addView(web, FrameLayout.LayoutParams(-1, -1))
        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(col(R.color.accent)); visibility = View.GONE }
        content.addView(bar, FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP))

        searchBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM; setBackgroundColor(col(R.color.bg)); setPadding(dp(16), dp(8), dp(16), dp(12)); visibility = View.GONE; isClickable = true }
        cards = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        searchBox.addView(cards)
        val inRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = round(col(R.color.pill), 14); setPadding(dp(12), 0, dp(12), 0) }
        inRow.addView(icon(R.drawable.ic_search, 18, col(R.color.sub)))
        input = EditText(this).apply {
            background = null; textSize = 15f; setTextColor(col(R.color.text)); setHintTextColor(col(R.color.sub)); hint = "Search or enter address"
            setSingleLine(); imeOptions = EditorInfo.IME_ACTION_GO; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(e: Editable?) { refreshCards() }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })
            setOnEditorActionListener { _, a, e -> if (a == EditorInfo.IME_ACTION_GO || e?.keyCode == KeyEvent.KEYCODE_ENTER) { go(text.toString()); true } else false }
        }
        inRow.addView(input, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = dp(8) })
        inRow.addView(icon(R.drawable.ic_close, 18, col(R.color.sub)).apply { setOnClickListener { input.setText("") } })
        searchBox.addView(inRow, LinearLayout.LayoutParams(-1, dp(44)))
        content.addView(searchBox, FrameLayout.LayoutParams(-1, -1))

        bottom = LinearLayout(this).apply { setBackgroundColor(col(R.color.card)); gravity = Gravity.CENTER_VERTICAL }
        listOf(R.drawable.ic_history, R.drawable.ic_download, R.drawable.ic_share, R.drawable.ic_bookmark, R.drawable.ic_tabs).forEachIndexed { i, r ->
            val f = FrameLayout(this).apply { setOnClickListener { bottomTap(i) } }
            f.addView(icon(r, 22), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
            bottom.addView(f, LinearLayout.LayoutParams(0, dp(52), 1f))
        }
        root.addView(bottom, LinearLayout.LayoutParams(-1, -2))

        web.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; mediaPlaybackRequiresUserGesture = false; builtInZoomControls = true; displayZoomControls = false }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val u = r.url; if (u.scheme == "http" || u.scheme == "https") return false
                try { startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (e: Exception) {}; return true
            }
            override fun onPageStarted(v: WebView, u: String?, f: android.graphics.Bitmap?) { title.text = host(u) }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView, p: Int) { bar.progress = p; bar.visibility = if (p < 100) View.VISIBLE else View.GONE }
        }
        web.setDownloadListener { url, ua, cd, mime, _ -> startDownload(url, ua, cd, mime) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    searching -> closeSearch()
                    web.visibility == View.VISIBLE && web.canGoBack() -> web.goBack()
                    web.visibility == View.VISIBLE -> showHome()
                    else -> finish()
                }
            }
        })
        ContextCompat.registerReceiver(this, rx, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        if (s != null && web.restoreState(s) != null && web.url != null) { web.visibility = View.VISIBLE; home.visibility = View.GONE; title.text = host(web.url) }
        else intent?.data?.let { load(it.toString()) }
    }

    override fun onNewIntent(i: Intent) { super.onNewIntent(i); i.data?.let { load(it.toString()) } }
    override fun onSaveInstanceState(o: Bundle) { super.onSaveInstanceState(o); web.saveState(o) }
    override fun onResume() {
        super.onResume()
        web.importantForAutofill = if (Store.flag(this, "autofill", false)) View.IMPORTANT_FOR_AUTOFILL_YES else View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
    override fun onDestroy() { unregisterReceiver(rx); super.onDestroy() }

    private fun host(u: String?) = Uri.parse(u ?: "").host?.removePrefix("www.") ?: "SGO"
    private fun load(u: String) { web.visibility = View.VISIBLE; home.visibility = View.GONE; web.loadUrl(u) }
    private fun showHome() { closeSearch(); web.visibility = View.GONE; home.visibility = View.VISIBLE; title.text = "SGO" }

    private fun go(t: String) {
        if (t.isBlank()) return
        Store.addHistory(this, t.trim()); closeSearch(); load(Util.resolve(this, t))
    }
    private fun openSearch() {
        searching = true; home.visibility = View.GONE; bottom.visibility = View.GONE; searchBox.visibility = View.VISIBLE
        input.setText(""); refreshCards(); input.requestFocus()
        input.post { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, 0) }
    }
    private fun closeSearch() {
        if (!searching) return
        searching = false; searchBox.visibility = View.GONE; bottom.visibility = View.VISIBLE
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0)
        if (web.visibility != View.VISIBLE) home.visibility = View.VISIBLE
    }
    private fun refreshCards() {
        cards.removeAllViews()
        val q = input.text.toString().trim()
        val hist = if (Store.flag(this, "sugg", true)) Store.list(this, "hist").map { it to false } else emptyList()
        val fav = if (Store.flag(this, "ssugg", true)) Store.list(this, "fav").map { it to true } else emptyList()
        val all = if (Store.flag(this, "best", true)) fav + hist else hist + fav
        all.filter { q.isEmpty() || it.first.contains(q, true) }.distinctBy { it.first }.take(4).forEach { (t, star) ->
            val c = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = round(col(R.color.card), 10); setPadding(dp(14), dp(10), dp(14), dp(10)); elevation = dp(2).toFloat(); setOnClickListener { go(t) } }
            c.addView(icon(if (star) R.drawable.ic_star else R.drawable.ic_history, 16))
            c.addView(tv(t, 13f).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(12), 0, 0, 0) })
            cards.addView(c, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
    }

    private fun bottomTap(i: Int) {
        when (i) {
            0 -> listDialog("History", Store.list(this, "hist")) { go(it) }
            1 -> startActivity(Intent(this, DownloadsActivity::class.java))
            2 -> { val u = web.url; if (web.visibility == View.VISIBLE && u != null) startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, u), "Share")) else Toast.makeText(this, "Open a page first", Toast.LENGTH_SHORT).show() }
            3 -> {
                val f = Store.list(this, "fav")
                val items = (listOf("★ Add this page") + f).toTypedArray()
                AlertDialog.Builder(this).setTitle("Favorites").setItems(items) { _, k ->
                    if (k == 0) { val u = web.url; if (web.visibility == View.VISIBLE && u != null) { f.remove(u); f.add(0, u); Store.saveList(this, "fav", f); Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show() } }
                    else go(f[k - 1])
                }.setNeutralButton("Remove all") { _, _ -> Store.saveList(this, "fav", emptyList()) }.show()
            }
            4 -> showHome()
        }
    }
    private fun listDialog(t: String, l: List<String>, pick: (String) -> Unit) {
        val b = AlertDialog.Builder(this).setTitle(t)
        if (l.isEmpty()) b.setMessage("Nothing here yet") else b.setItems(l.toTypedArray()) { _, k -> pick(l[k]) }
        b.show()
    }

    private fun startDownload(url: String, ua: String?, cd: String?, mime: String?) {
        val dir = getExternalFilesDir("Download")!!
        var name = URLUtil.guessFileName(url, cd, mime)
        if (File(dir, name).exists()) name = "${System.currentTimeMillis() / 1000}_$name"
        val req = DownloadManager.Request(Uri.parse(url)).setMimeType(mime)
            .addRequestHeader("User-Agent", ua ?: "").addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, "Download", name)
        val id = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
        Store.addDownload(this, Dl(id, name, File(dir, name).absolutePath))
        Toast.makeText(this, "Downloading $name", Toast.LENGTH_SHORT).show()
    }
}
