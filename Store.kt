package com.sgo.browser

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONArray
import org.json.JSONObject

data class Dl(val id: Long, val name: String, val path: String)

object Store {
    private fun p(c: Context) = c.getSharedPreferences("sgo", 0)
    fun flag(c: Context, k: String, d: Boolean) = p(c).getBoolean(k, d)
    fun setFlag(c: Context, k: String, v: Boolean) = p(c).edit().putBoolean(k, v).apply()
    fun str(c: Context, k: String, d: String) = p(c).getString(k, d) ?: d
    fun setStr(c: Context, k: String, v: String) = p(c).edit().putString(k, v).apply()

    val engines = linkedMapOf(
        "Google" to "https://www.google.com/search?q=", "DuckDuckGo" to "https://duckduckgo.com/?q=",
        "Bing" to "https://www.bing.com/search?q=", "Brave" to "https://search.brave.com/search?q=")
    fun engine(c: Context) = str(c, "engine", "Google")

    fun list(c: Context, k: String): MutableList<String> {
        val a = JSONArray(p(c).getString(k, "[]")); return MutableList(a.length()) { a.getString(it) }
    }
    fun saveList(c: Context, k: String, l: List<String>) = p(c).edit().putString(k, JSONArray(l).toString()).apply()
    fun addHistory(c: Context, q: String) { val l = list(c, "hist"); l.remove(q); l.add(0, q); saveList(c, "hist", l.take(100)) }

    fun downloads(c: Context): List<Dl> {
        val a = JSONArray(p(c).getString("dls", "[]"))
        return (0 until a.length()).map { val o = a.getJSONObject(it); Dl(o.getLong("id"), o.getString("name"), o.getString("path")) }.reversed()
    }
    private fun saveDls(c: Context, l: List<Dl>) {
        val a = JSONArray(); l.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("path", it.path)) }
        p(c).edit().putString("dls", a.toString()).apply()
    }
    fun addDownload(c: Context, d: Dl) = saveDls(c, downloads(c).reversed() + d)
    fun removeDownload(c: Context, id: Long) = saveDls(c, downloads(c).reversed().filter { it.id != id })

    fun applyTheme(c: Context) = AppCompatDelegate.setDefaultNightMode(when (str(c, "theme", "auto")) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO; "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM })
}
