package com.me.ampere

import android.content.Context
import android.graphics.*
import android.view.View

class GaugeView(c: Context) : View(c) {
    private var pct = 0f
    private var ma = 0f
    private var label = ""
    private var accent = Color.parseColor("#00E5A0")
    private val d = resources.displayMetrics.density

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 14 * d
        color = Color.parseColor("#1FFFFFFF"); strokeCap = Paint.Cap.ROUND
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 14 * d; strokeCap = Paint.Cap.ROUND
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 28 * d; strokeCap = Paint.Cap.ROUND
    }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 58 * d
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99FFFFFF"); textAlign = Paint.Align.CENTER; textSize = 15 * d
    }
    private val pctP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; textSize = 22 * d
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    fun update(pct: Float, ma: Float, label: String, accent: Int) {
        this.pct = pct; this.ma = ma; this.label = label; this.accent = accent
        invalidate()
    }

    override fun onDraw(cv: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val rad = minOf(width, height) / 2f - 24 * d
        val r = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        cv.drawArc(r, 135f, 270f, false, track)
        val sweep = 270f * pct.coerceIn(0f, 100f) / 100f
        glow.color = accent; glow.alpha = 45
        arc.color = accent
        cv.drawArc(r, 135f, sweep, false, glow)
        cv.drawArc(r, 135f, sweep, false, arc)
        val sign = if (ma > 0) "+" else ""
        cv.drawText(sign + ma.toInt().toString(), cx, cy + 14 * d, big)
        cv.drawText("mA", cx, cy + 40 * d, small)
        pctP.color = accent
        cv.drawText(pct.toInt().toString() + "%", cx, cy - 56 * d, pctP)
        cv.drawText(label, cx, cy + rad - 6 * d, small)
    }
}

class GraphView(c: Context) : View(c) {
    private val pts = ArrayList<Float>()
    private var accent = Color.parseColor("#00E5A0")
    private val d = resources.displayMetrics.density
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2.5f * d; strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#14FFFFFF"); strokeWidth = 1f * d
    }

    fun add(v: Float, accent: Int) {
        this.accent = accent
        pts.add(v)
        if (pts.size > 90) pts.removeAt(0)
        invalidate()
    }

    override fun onDraw(cv: Canvas) {
        if (pts.size < 2) return
        val w = width.toFloat()
        val h = height.toFloat()
        var mn = pts.min()
        var mx = pts.max()
        if (mx - mn < 50f) { val m = (mx + mn) / 2f; mn = m - 25f; mx = m + 25f }
        val pad = 8 * d
        val stepX = w / 89f
        fun y(v: Float) = h - pad - (v - mn) / (mx - mn) * (h - 2 * pad)
        for (k in 1..3) cv.drawLine(0f, h * k / 4f, w, h * k / 4f, grid)
        val path = Path()
        var firstX = 0f
        pts.forEachIndexed { i, v ->
            val x = w - (pts.size - 1 - i) * stepX
            if (i == 0) { path.moveTo(x, y(v)); firstX = x } else path.lineTo(x, y(v))
        }
        val area = Path(path)
        area.lineTo(w, h); area.lineTo(firstX, h); area.close()
        fill.shader = LinearGradient(0f, 0f, 0f, h,
            (accent and 0x00FFFFFF) or (0x5A shl 24), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        cv.drawPath(area, fill)
        line.color = accent
        cv.drawPath(path, line)
    }
}
