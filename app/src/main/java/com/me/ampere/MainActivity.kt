package com.me.ampere

import android.app.Activity
import android.content.*
import android.os.*
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var tv: TextView
    private val h = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val ma = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val mv = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val pct = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val t = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
            tv.text = "$pct%\n${ma / 1000} mA\n${"%.2f".format(mv / 1000.0)} V\n$t °C"
            h.postDelayed(this, 1000)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        tv = TextView(this).apply { textSize = 36f; gravity = Gravity.CENTER }
        setContentView(tv)
    }
    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }
}
