package com.ruqaiyapro.tuner

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

object HardwareTuner {

    private const val TAG = "HardwareTuner"
    private val handler = Handler(Looper.getMainLooper())

    fun applyInfinixOptimizations(context: Context) {
        val model = Build.MODEL
        Log.i(TAG, "Device Model: $model (Infinix HOT12 Play X6816C detected: ${model.contains("X6816", true)})")

        // Schedule aggressive RAM trim every 15 seconds
        handler.post(object : Runnable {
            override fun run() {
                trimMemoryAndFreeRam(context)
                handler.postDelayed(this, 15000)
            }
        })
    }

    fun trimMemoryAndFreeRam(context: Context) {
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            val freeMb = memInfo.availMem / (1024 * 1024)
            if (freeMb < 500) {
                System.gc()
                Runtime.getRuntime().gc()
                Log.d(TAG, "Aggressive RAM cleanup triggered! Free memory was: ${freeMb}MB")
            }
        } catch (e: Exception) {
            Log.e(TAG, "RAM cleanup error", e)
        }
    }
}