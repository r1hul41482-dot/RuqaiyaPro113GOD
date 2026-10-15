package com.ruqaiyapro.debug.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceAssistant(private val ctx: Context, private val core: Core) : TextToSpeech.OnInitListener {

    var listening by mutableStateOf(false)
    var transcript by mutableStateOf("")
    var reply by mutableStateOf("")
    var lastWasAi by mutableStateOf(false)
    var lastPrompt by mutableStateOf("")

    private var tts: TextToSpeech? = TextToSpeech(ctx, this)
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null
    private val main = Handler(Looper.getMainLooper())

    private val shayari = listOf(
        "Shokal er alo tomar hashir moto, Boss Rubel - ajker din hok shundor.",
        "Chander alo kom pore jay, kintu Boss Rubel er shopno kokhono thame na.",
        "Borsha namuk ba rod uthuk, Boss Rubel er mon thak shanto ar joyi."
    )
    private val koutuk = listOf(
        "Phone charge-e boshiye bolo - 1%. Phone bole: ami to tomar mot, charge na pele boshe thaki.",
        "Student: Sir, ami exam-e fail kore feli ni, sudhu pass korte deri hocche.",
        "Wifi bole: ami dur theke tomake bhalobashi. Tumi kachhe ashle ami hariye jai."
    )
    private val roast = listOf(
        "Boss, tomar RAM 7GB, kintu bhulo mon-e sudhu 1GB!",
        "Boss, tumi eto slow, loading bar o tomake dekhe lojja pay.",
        "Boss, tomar selfie dekhe camera nijei autofocus off kore dey - mojar kotha!"
    )

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            try {
                tts?.setPitch(1.15f)
                val r = tts?.setLanguage(Locale("bn", "BD"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
            } catch (e: Exception) { }
        }
    }

    fun speak(text: String) {
        if (!ttsReady) return
        try { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ruqaiya") } catch (e: Exception) { }
    }

    fun startListening() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
                reply = "Speech recognition engine nai. Google app install/update koro."
                return
            }
            // No-Beep trick
            core.device.muteBeepStreams()
            main.postDelayed({ core.device.unmuteBeepStreams() }, 900)

            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { listening = false }
                    override fun onError(error: Int) {
                        listening = false
                        reply = "Voice error ($error). Abar chesta koro ba text likhe command dao."
                    }
                    override fun onResults(results: Bundle?) {
                        listening = false
                        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        handle(list?.firstOrNull() ?: "")
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        transcript = "Tumi bolcho: " + (list?.firstOrNull() ?: "")
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, core.voiceLang)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            recognizer?.startListening(intent)
            listening = true
        } catch (e: Exception) {
            listening = false
            reply = "Mic start hoy ni: ${e.message}"
        }
    }

    fun stopListening() {
        try { recognizer?.stopListening() } catch (e: Exception) { }
        listening = false
    }

    fun handle(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        transcript = "Tumi bolle: $text"
        core.logHud("Tumi bolle: $text")
        core.scope.launch {
            val (answer, ai) = try {
                route(text.lowercase(Locale.getDefault()), text)
            } catch (e: Exception) {
                Pair("Error: ${e.message}", false)
            }
            reply = answer
            lastWasAi = ai
            lastPrompt = text
            core.logHud("Ruqaiya: $answer")
            core.logActivity("VOICE", "Command: $text", answer, "SUCCESS")
            speak(answer)
        }
    }

    private fun openIntent(intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        true
    } catch (e: Exception) { false }

    private suspend fun route(t: String, raw: String): Pair<String, Boolean> {
        fun has(vararg k: String) = k.any { t.contains(it) }

        return when {
            has("what time", "koyta baje", "ki somoy", "somoy koto", "সময়") ->
                Pair("Ekhon somoy " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()), false)

            has("what date", "ajker tarikh", "aj ki tarikh", "tarikh", "তারিখ") ->
                Pair("Ajker tarikh " + SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date()), false)

            has("flash", "torch", "ফ্ল্যাশ", "টর্চ") -> {
                val off = has(" off", "bondho", "nibhao", "বন্ধ", "নিভা")
                val ok = core.device.setTorch(!off)
                Pair(if (!ok) "Torch control kora gelo na (camera busy hote pare)." else if (off) "Flash bondho korlam." else "Flash jalalam.", false)
            }

            has("amar naam", "my name", "আমার নাম") ->
                Pair("Apnar naam Boss ${core.ownerName}. Ami Ruqaiya.", false)

            has("phone kor", "call koro", "ফোন কর") ->
                Pair(if (openIntent(Intent(Intent.ACTION_DIAL))) "Dialer khulchi." else "Dialer khulte parlam na.", false)

            has("camera", "ক্যামেরা") ->
                Pair(if (openIntent(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))) "Camera khulchi." else "Camera khulte parlam na.", false)

            has("whatsapp", "হোয়াটসঅ্যাপ") -> {
                val i = ctx.packageManager.getLaunchIntentForPackage("com.whatsapp")
                Pair(if (i != null && openIntent(i)) "WhatsApp khulchi." else "WhatsApp paini.", false)
            }

            has("wifi", "ওয়াইফাই") -> Pair(if (openIntent(core.device.wifiIntent())) "Wi-Fi panel khulchi." else "Wi-Fi panel khulte parlam na.", false)

            has("bluetooth", "ব্লুটুথ") -> Pair(if (openIntent(core.device.bluetoothIntent())) "Bluetooth settings khulchi." else "Bluetooth settings khulte parlam na.", false)

            has("battery saver", "ultra saver") -> {
                core.setUltraSaver(!core.ultraSaver)
                Pair(if (core.ultraSaver) "Battery ultra saver ON." else "Battery ultra saver OFF.", false)
            }

            has("shayari", "shayri", "শায়রি", "কবিতা") ->
                askOrLocal("Boss ${core.ownerName} er jonno ekta shundor shayari bolo, 4 line, Bangla (Banglish ba Bangla lipi).", shayari.random())

            has("koutuk", "joke", "কৌতুক") ->
                askOrLocal("Ekta chhoto mojar koutuk bolo, Bangla/Banglish-e.", koutuk.random())

            has("roast", "রোস্ট") ->
                askOrLocal("Boss ${core.ownerName} ke halka mojar roast koro (kono offensive na), 2 line.", roast.random())

            else -> askOrLocal(raw, "Brain OFF. Settings > Universal Brain-e API key dile ami sob proshner uttor dibo.")
        }
    }

    private suspend fun askOrLocal(prompt: String, fallback: String): Pair<String, Boolean> {
        if (!core.brain.isBrainActive()) return Pair(fallback, false)
        val persona = "You are Ruqaiya, a friendly female voice assistant for Boss ${core.ownerName}. " +
            "Reply short (max 3 sentences) in Bangla or Banglish." + core.feedback.getPromptContextPrefix()
        val r = core.brain.generate(prompt, persona)
        core.logActivity("AI_GENERATION", "Brain reply", r.text.take(200), if (r.success) "SUCCESS" else "FAILED", r.provider.displayName)
        return Pair(r.text, r.success)
    }
}
