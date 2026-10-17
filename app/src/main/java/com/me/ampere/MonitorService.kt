package com.me.ampere

import android.app.*
import android.content.*
import android.os.*
import kotlin.math.abs

class MonitorService : Service() {
    private val h = Handler(Looper.getMainLooper())
    private val win = ArrayList<Float>()
    private var ema = Float.NaN
    private var last: Boolean? = null
    private var n = 0
    private lateinit var nm: NotificationManager

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel("amp", "Live current", NotificationManager.IMPORTANCE_LOW)
        ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)
        startForeground(1, build("Reading...", ""))
        h.post(tick)
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun build(title: String, text: String): Notification =
        Notification.Builder(this, "amp")
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .build()

    private val tick = object : Runnable {
        override fun run() {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val mv = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val pct = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            var raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toFloat()
            if (abs(raw) > 10000f) raw /= 1000f
            raw = if (charging) abs(raw) else -abs(raw)

            if (last != charging) { win.clear(); ema = Float.NaN; last = charging }
            win.add(raw)
            if (win.size > 15) win.removeAt(0)
            val med = win.sorted()[win.size / 2]
            ema = if (ema.isNaN()) med else ema + 0.15f * (med - ema)

            val v = mv / 1000f
            val w = v * abs(ema) / 1000f
            val sign = if (ema > 0) "+" else ""
            val title = (if (charging) "Charging  " else "Discharging  ") +
                    sign + ema.toInt() + " mA"
            val text = "%.2f V  |  %.1f W  |  %d%%  |  %.1f°C".format(v, w, pct, temp)
            if (++n % 4 == 0) nm.notify(1, build(title, text))
            h.postDelayed(this, 250)
        }
    }
}
