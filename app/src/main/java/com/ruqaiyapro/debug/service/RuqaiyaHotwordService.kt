package com.ruqaiyapro.debug.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

// Placeholder: hotword logic can be added here later.
class RuqaiyaHotwordService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }
}
