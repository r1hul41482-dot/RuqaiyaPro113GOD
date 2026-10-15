package com.ruqaiyapro.debug

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.util.Log
import com.ruqaiyapro.debug.activity.RuqaiyaDatabase

class RuqaiyaApp : Application() {

    companion object {
        const val TAG = "RuqaiyaApp"
        const val CHANNEL_HOTWORD = "ruqaiya_hotword_channel"
        lateinit var instance: RuqaiyaApp
            private set
    }

    lateinit var database: RuqaiyaDatabase
        private set

    val core: com.ruqaiyapro.debug.core.Core by lazy { com.ruqaiyapro.debug.core.Core(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            database = RuqaiyaDatabase.getDatabase(this)
            createNotificationChannel()
            checkRamSafety()
            Log.d(TAG, "RuqaiyaApp initialized successfully for Boss Rubel on Infinix HOT 12 Play")
        } catch (e: Exception) {
            Log.e(TAG, "Fatal init error guarded: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_HOTWORD,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(false)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun checkRamSafety(): Long {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val freeMb = (freeMemory + (maxMemory - totalMemory)) / (1024 * 1024)

        if (freeMb < 500) {
            Log.w(TAG, "Low RAM warning: ${freeMb}MB free. Triggering GC for Infinix HOT 12 Play 4+3GB.")
            System.gc()
        } else {
            Log.i(TAG, "RAM safety OK: ${freeMb}MB available memory pool.")
        }
        return freeMb
    }
}
