package com.sgo.browser

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()
fun Context.col(id: Int) = ContextCompat.getColor(this, id)
fun Context.round(color: Int, r: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }
fun Context.icon(res: Int, size: Int = 22, tint: Int = col(R.color.text)) = ImageView(this).apply {
    setImageResource(res); setColorFilter(tint); layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
}
fun Context.tv(t: String, size: Float = 14f, color: Int = col(R.color.text)) = TextView(this).apply {
    text = t; textSize = size; setTextColor(color)
}

fun Context.header(title: String, onClose: () -> Unit): View {
    val f = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(-1, dp(52)) }
    f.addView(tv(title, 16f).apply { gravity = Gravity.CENTER }, FrameLayout.LayoutParams(-1, -1))
    val x = icon(R.drawable.ic_close, 20, col(R.color.text)).apply { setOnClickListener { onClose() } }
    f.addView(x, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER_VERTICAL or Gravity.END).apply { marginEnd = dp(18) })
    return f
}

fun Context.card(parent: LinearLayout, title: String): LinearLayout {
    val c = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = round(col(R.color.card), 12); setPadding(dp(16), dp(14), dp(16), dp(10)); elevation = dp(1).toFloat()
    }
    c.addView(tv(title, 13f, col(R.color.accent)).apply { setPadding(0, 0, 0, dp(6)) })
    parent.addView(c, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    return c
}

fun Context.row(card: LinearLayout, label: String, right: View?, click: (() -> Unit)?) {
    val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(40) }
    r.addView(tv(label, 13f), LinearLayout.LayoutParams(0, -2, 1f))
    if (right != null) r.addView(right)
    if (click != null) r.setOnClickListener { click() }
    card.addView(r, LinearLayout.LayoutParams(-1, -2))
}

fun Context.valueRow(card: LinearLayout, label: String, value: String, click: () -> Unit) {
    val box = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
    box.addView(tv(value, 12f, col(R.color.sub)))
    box.addView(icon(R.drawable.ic_chevron, 16, col(R.color.text)))
    row(card, label, box, click)
}
