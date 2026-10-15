package com.ruqaiyapro.debug.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ruqaiyapro.debug.RuqaiyaApp
import com.ruqaiyapro.debug.activity.ActivityLogEntity
import com.ruqaiyapro.debug.brain.BrainManager
import com.ruqaiyapro.debug.feedback.FeedbackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared state + managers for the whole app. */
class Core(val app: RuqaiyaApp) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val prefs: SharedPreferences = app.getSharedPreferences("ruqaiya_prefs", Context.MODE_PRIVATE)

    val brain = BrainManager(app)
    val feedback = FeedbackManager(app)
    val device = DeviceController(app)
    val assistant: VoiceAssistant by lazy { VoiceAssistant(app, this) }

    val hud = mutableStateListOf<String>()
    var freeRamMb by mutableStateOf(0L)
    var totalRamMb by mutableStateOf(0L)
    var brightness by mutableStateOf(0.7f)
    var ultraSaver by mutableStateOf(prefs.getBoolean("ultra_saver", false))
    var chibiRunning by mutableStateOf(false)
    var ownerName by mutableStateOf(prefs.getString("owner_name", "Rubel") ?: "Rubel")
    var voiceLang by mutableStateOf(prefs.getString("voice_lang", "bn-BD") ?: "bn-BD")

    private val main = Handler(Looper.getMainLooper())

    fun setUltraSaver(on: Boolean) {
        ultraSaver = on
        prefs.edit().putBoolean("ultra_saver", on).apply()
        if (on) device.setStrobe(false)
        logHud(if (on) "Battery 1% Ultra Saver ON" else "Battery Ultra Saver OFF")
    }

    fun setVoiceLang(lang: String) {
        voiceLang = lang
        prefs.edit().putString("voice_lang", lang).apply()
    }

    fun setOwnerName(name: String) {
        ownerName = name
        prefs.edit().putString("owner_name", name).apply()
    }

    fun logHud(line: String) {
        val t = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        main.post {
            hud.add(0, "$t  $line")
            while (hud.size > 60) hud.removeAt(hud.size - 1)
        }
    }

    fun logActivity(type: String, title: String, desc: String, status: String = "INFO", provider: String? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                app.database.activityDao().insertLog(
                    ActivityLogEntity(eventType = type, title = title, description = desc, status = status, providerName = provider)
                )
            } catch (e: Exception) { /* never crash for logging */ }
        }
    }
}
