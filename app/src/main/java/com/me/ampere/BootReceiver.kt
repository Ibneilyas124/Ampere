package com.me.ampere

import android.content.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (c.getSharedPreferences("ampere", Context.MODE_PRIVATE).getBoolean("on", true))
            c.startForegroundService(Intent(c, MonitorService::class.java))
    }
}
