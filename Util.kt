package com.sgo.browser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object Util {
    fun resolve(c: Context, input: String): String {
        val t = input.trim()
        if (t.startsWith("http://") || t.startsWith("https://")) return t
        if (Store.flag(c, "quick", true) && !t.contains(" ") && t.contains(".")) return "https://$t"
        return (Store.engines[Store.engine(c)] ?: Store.engines["Google"]!!) + Uri.encode(t)
    }
    private fun uri(a: Activity, f: File) = FileProvider.getUriForFile(a, a.packageName + ".files", f)

    fun install(a: Activity, f: File) {
        if (!f.exists()) { Toast.makeText(a, "File not found", Toast.LENGTH_SHORT).show(); return }
        if (!a.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(a, "Allow SGO to install apps, then tap Install again", Toast.LENGTH_LONG).show()
            a.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.packageName))); return
        }
        a.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri(a, f), "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun open(a: Activity, f: File) {
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
        try { a.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri(a, f), mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (e: Exception) { Toast.makeText(a, "No app can open this file", Toast.LENGTH_SHORT).show() }
    }
    fun size(b: Long): String = when {
        b <= 0 -> ""
        b < 1024 * 1024 -> "${b / 1024} KB"
        else -> String.format("%.1f MB", b / 1048576.0)
    }
}
