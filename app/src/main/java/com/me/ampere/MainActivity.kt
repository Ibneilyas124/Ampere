package com.me.ampere

import android.app.Activity
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import kotlin.math.abs

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var gauge: GaugeView
    private lateinit var graph: GraphView
    private lateinit var tVolt: TextView
    private lateinit var tTemp: TextView
    private lateinit var tPower: TextView
    private lateinit var tPlug: TextView

    private val win = ArrayList<Float>()
    private var ema = Float.NaN
    private var lastCharging: Boolean? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun card(title: String, v: TextView): LinearLayout {
        val t = TextView(this).apply {
            text = title; textSize = 12f; setTextColor(Color.parseColor("#8FA0B8"))
        }
        v.apply {
            textSize = 22f; setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat(); setColor(Color.parseColor("#151C2C"))
            }
            addView(t); addView(v)
        }
    }

    private fun row(a: View, b: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        val l1 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        l1.rightMargin = dp(6)
        val l2 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        l2.leftMargin = dp(6)
        addView(a, l1); addView(b, l2)
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(12)
        layoutParams = lp
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val bg = Color.parseColor("#0B1220")
        window.statusBarColor = bg
        window.navigationBarColor = bg

        gauge = GaugeView(this)
        graph = GraphView(this)
        tVolt = TextView(this); tTemp = TextView(this)
        tPower = TextView(this); tPlug = TextView(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(28), dp(16), dp(24))
        }
        root.addView(TextView(this).apply {
            text = "Ampere"; textSize = 26f; setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        root.addView(gauge, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(300)))

        val gTitle = TextView(this).apply {
            text = "LIVE CURRENT"; textSize = 12f; setTextColor(Color.parseColor("#8FA0B8"))
        }
        val gCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat(); setColor(Color.parseColor("#151C2C"))
            }
            addView(gTitle)
            addView(graph, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(110)))
        }
        root.addView(gCard)
        root.addView(row(card("VOLTAGE", tVolt), card("TEMPERATURE", tTemp)))
        root.addView(row(card("POWER", tPower), card("SOURCE", tPlug)))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(bg); addView(root)
        })
    }

    private val tick = object : Runnable {
        override fun run() {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val mv = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val pct = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            var raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toFloat()
            if (abs(raw) > 10000f) raw /= 1000f          // microamp -> mA
            raw = if (charging) abs(raw) else -abs(raw)  // sign normalize

            if (lastCharging != charging) {
                win.clear(); ema = Float.NaN; lastCharging = charging
            }
            win.add(raw)
            if (win.size > 7) win.removeAt(0)
            val med = win.sorted()[win.size / 2]          // spike filter
            ema = if (ema.isNaN()) med else ema + 0.2f * (med - ema)  // smoothing

            val volts = mv / 1000f
            val watts = volts * abs(ema) / 1000f
            val accent = Color.parseColor(if (charging) "#00E5A0" else "#FF8A5B")
            val plug = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Battery"
            }
            gauge.update(pct.toFloat(), ema,
                if (charging) "Charging" else "Discharging", accent)
            graph.add(ema, accent)
            tVolt.text = "%.2f V".format(volts)
            tTemp.text = "%.1f °C".format(temp)
            tPower.text = "%.1f W".format(watts)
            tPlug.text = plug
            h.postDelayed(this, 500)
        }
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }
}
