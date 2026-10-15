package com.ruqaiyapro.debug.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DeviceController(private val ctx: Context) {

    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val camMgr = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var strobeJob: Job? = null

    var torchOn by mutableStateOf(false)
    var strobeOn by mutableStateOf(false)

    private val beepStreams = intArrayOf(
        AudioManager.STREAM_MUSIC, AudioManager.STREAM_SYSTEM, AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_ALARM, AudioManager.STREAM_RING, AudioManager.STREAM_DTMF
    )

    // ---- No-Beep trick: mute listen-beep streams, unmute after 900ms
    fun muteBeepStreams() {
        for (s in beepStreams) {
            try { audio.adjustStreamVolume(s, AudioManager.ADJUST_MUTE, 0) } catch (e: Exception) { }
        }
    }

    fun unmuteBeepStreams() {
        for (s in beepStreams) {
            try { audio.adjustStreamVolume(s, AudioManager.ADJUST_UNMUTE, 0) } catch (e: Exception) { }
        }
    }

    // ---- Volume
    fun getVolumePercent(): Int = try {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        (audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / max).roundToInt()
    } catch (e: Exception) { 0 }

    fun setVolumePercent(percent: Int) {
        try {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (max * percent / 100f).roundToInt(), 0)
        } catch (e: Exception) { }
    }

    // ---- Torch
    private fun flashCameraId(): String? = try {
        camMgr.cameraIdList.firstOrNull {
            camMgr.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) { null }

    fun setTorch(on: Boolean): Boolean {
        val id = flashCameraId() ?: return false
        return try {
            camMgr.setTorchMode(id, on)
            torchOn = on
            true
        } catch (e: Exception) { false }
    }

    fun setStrobe(on: Boolean) {
        strobeJob?.cancel()
        strobeOn = on
        if (on) {
            strobeJob = scope.launch {
                var state = false
                while (isActive) {
                    state = !state
                    setTorch(state)
                    delay(120)
                }
            }
        } else {
            setTorch(false)
        }
    }

    // ---- RAM
    fun ramInfo(): Pair<Long, Long> {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return Pair(mi.availMem / 1048576L, mi.totalMem / 1048576L)
    }

    // ---- Wi-Fi / Bluetooth (Android 10+ does not allow apps to toggle them directly)
    fun wifiIntent(): Intent {
        val i = if (Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        else Intent(Settings.ACTION_WIFI_SETTINGS)
        return i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun bluetoothIntent(): Intent =
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun vibrate(ms: Long) {
        try {
            val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(ms)
        } catch (e: Exception) { }
    }
}
