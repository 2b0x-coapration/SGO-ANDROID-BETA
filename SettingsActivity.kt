package com.sgo.browser

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowInsetsControllerCompat

class SettingsActivity : AppCompatActivity() {
    private fun sw(card: LinearLayout, label: String, key: String, def: Boolean) {
        val s = SwitchCompat(this).apply {
            isChecked = Store.flag(this@SettingsActivity, key, def)
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(col(R.color.accent), col(R.color.track)))
            setOnCheckedChangeListener { _, b -> Store.setFlag(this@SettingsActivity, key, b) }
        }
        row(card, label, s, null)
    }

    override fun onCreate(s: Bundle?) {
        Store.applyTheme(this); super.onCreate(s)
        window.statusBarColor = col(R.color.bg); window.navigationBarColor = col(R.color.bg)
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(col(R.color.bg)); fitsSystemWindows = true }
        setContentView(root)
        WindowInsetsControllerCompat(window, root).apply { isAppearanceLightStatusBars = !night; isAppearanceLightNavigationBars = !night }
        root.addView(header("Settings") { finish() })
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(8), dp(14), dp(20)) }
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))

        val ap = card(body, "Appearance")
        val g = RadioGroup(this)
        listOf("auto" to "Automatic", "light" to "Light Theme", "dark" to "Dark Theme").forEach { (k, l) ->
            g.addView(RadioButton(this).apply {
                text = l; textSize = 13f; setTextColor(col(R.color.text)); buttonTintList = ColorStateList.valueOf(col(R.color.accent))
                id = View.generateViewId(); isChecked = Store.str(this@SettingsActivity, "theme", "auto") == k
                setOnClickListener { Store.setStr(this@SettingsActivity, "theme", k); Store.applyTheme(this@SettingsActivity) }
            })
        }
        ap.addView(g)

        val ge = card(body, "General")
        valueRow(ge, "Downloads", "Phone") { startActivity(Intent(this, DownloadsActivity::class.java)) }
        valueRow(ge, "Favorites", "My Favorites") {
            val f = Store.list(this, "fav")
            val b = AlertDialog.Builder(this).setTitle("Favorites")
            if (f.isEmpty()) b.setMessage("No favorites yet") else b.setItems(f.toTypedArray(), null)
            b.show()
        }
        sw(ge, "AutoFill", "autofill", false)

        val se = card(body, "Search")
        valueRow(se, "Search Engine", Store.engine(this)) {
            val n = Store.engines.keys.toTypedArray()
            AlertDialog.Builder(this).setItems(n) { _, i -> Store.setStr(this, "engine", n[i]); recreate() }.show()
        }
        sw(se, "Search Engine Suggestions", "sugg", true)
        sw(se, "SGO Suggestions", "ssugg", true)
        sw(se, "Quick Website Search", "quick", true)
        sw(se, "Show Best Match First", "best", true)

        val pr = card(body, "Privacy and Security")
        row(pr, "Clear history", null) { Store.saveList(this, "hist", emptyList()); Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show() }
        row(pr, "Clear cookies and site data", null) {
            CookieManager.getInstance().removeAllCookies(null); WebStorage.getInstance().deleteAllData(); Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show() }
        row(pr, "Allow installing APKs", null) {
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) }
    }
}
