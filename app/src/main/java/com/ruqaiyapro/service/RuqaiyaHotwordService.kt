package com.ruqaiyapro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.ruqaiyapro.R
import com.ruqaiyapro.brain.BrainManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class RuqaiyaHotwordService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var audioManager: AudioManager
    private lateinit var tts: TextToSpeech
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isMuted = false

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this, this)

        // Acquire PARTIAL_WAKE_LOCK to prevent Android 11 screen-off killing
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RuqaiyaPro::HotwordWakeLock").apply {
            acquire(24 * 60 * 60 * 1000L) // 24 hours
        }

        startForegroundServiceNotification()
        setupSpeechRecognizer()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "ruqaiya_hotword_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ruqaiya Hotword Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Android 11 Safe Notification: NO FOREGROUND_SERVICE_MICROPHONE flag
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ruqaiya ghumacche...")
            .setContentText("Bolo Hey Ruqaiya")
            .setSmallIcon(R.drawable.ic_ruqaiya_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(113, notification)
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        startListeningWithNoBeep()
    }

    /**
     * No-Beep Trick: Mute STREAM_SYSTEM & STREAM_NOTIFICATION for 900ms before starting listening
     */
    private fun startListeningWithNoBeep() {
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
            isMuted = true

            speechRecognizer.startListening(recognizerIntent)

            Handler(Looper.getMainLooper()).postDelayed({
                if (isMuted) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                    isMuted = false
                }
            }, 900)
        } catch (e: Exception) {
            speechRecognizer.startListening(recognizerIntent)
        }
    }

    override fun onResults(results: android.os.Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotEmpty()) {
            handleVoiceCommand(text)
        }
        startListeningWithNoBeep()
    }

    private fun handleVoiceCommand(cmd: String) {
        val lower = cmd.lowercase()
        when {
            lower.contains("amar naam ki") -> speak("Apnar naam Boss Rubel, apnar shob kaje ami hazir!")
            lower.contains("amake mone rakho") -> speak("Boss Rubel ke ami konodin bhulbo na!")
            lower.contains("shayari") -> speak("Chokhe chokh rakhle shuru hoy golpo, Boss Rubel er jonno amar bhalobasha shobcheye shotyo!")
            lower.contains("roast") -> speak("Ore Boss, roast korle to apnar bondhura palanor jayga pabe na!")
            lower.contains("koutuk") -> speak("Ekjon doctor rogi ke bollen: shob thik ache, shudhu mathay ektu buddhir obhab!")
            else -> {
                // Dynamic AI Fallback if configured
                scope.launch {
                    val brain = BrainManager.getInstance(applicationContext)
                    if (brain.isBrainActive() && brain.isDynamicEnabled()) {
                        val reply = brain.askUniversalBrain(cmd).getOrDefault("Ji Boss Rubel, shunchi!")
                        speak(reply)
                    }
                }
            }
        }
    }

    private fun speak(message: String) {
        tts.setPitch(1.15f) // Sweet Female Pitch
        tts.setSpeechRate(1.02f)
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "ruqaiya_reply")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("bn", "BD")
        }
    }

    override fun onPartialResults(partialResults: android.os.Bundle?) {}
    override fun onReadyForSpeech(params: android.os.Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) {
        Handler(Looper.getMainLooper()).postDelayed({ startListeningWithNoBeep() }, 1000)
    }
    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.shutdown()
        if (wakeLock.isHeld) wakeLock.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}