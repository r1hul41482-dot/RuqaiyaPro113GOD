package com.ruqaiyapro.debug.features

import android.content.Context
import android.content.SharedPreferences

data class RuqaiyaFeature(
    val id: Int,
    val nameBn: String,
    val nameEn: String,
    val category: String, // Core, Khata&Face, Pro
    val triggerPhrase: String,
    val description: String,
    val defaultEnabled: Boolean = true
)

class Features113Manager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ruqaiya_prefs", Context.MODE_PRIVATE)

    fun isFeatureEnabled(featureId: Int): Boolean {
        return prefs.getBoolean("feature_$featureId", true)
    }

    fun setFeatureEnabled(featureId: Int, enabled: Boolean) {
        prefs.edit().putBoolean("feature_$featureId", enabled).apply()
    }

    fun getAllFeatures(): List<RuqaiyaFeature> {
        return listOf(
            RuqaiyaFeature(1, "অফলাইন ভোস্ক রিকগনিশন (ইন্টারনেট ছাড়া)", "Offline Vosk Recognition", "Core", "Offline e shuno", "Lightweight offline speech recognition without cellular data"),
            RuqaiyaFeature(2, "ব্যাটারি ১% আল্ট্রা সেভার মোড", "Battery 1% Ultra Saver", "Core", "Battery bacao", "Throttles background loops to 0.5s and dims screen to 1%"),
            RuqaiyaFeature(3, "স্ক্রিন অফ ব্যাকগ্রাউন্ড লিসেনিং", "Screen Off Background Listening", "Core", "Screen bondho te shuno", "Maintains PARTIAL_WAKE_LOCK to hear commands with display off"),
            RuqaiyaFeature(4, "নো-বীপ ট্রিক (৯০০ms মিউট)", "No-Beep Trick 900ms", "Core", "Hey Ruqaiya", "Silences Android audio stream during hotword trigger"),
            RuqaiyaFeature(5, "হোয়াটসঅ্যাপ অটো-রিপ্লাই লবি", "WhatsApp Auto-Reply Lobby", "Core", "WhatsApp Auto ON", "Sends busy message on behalf of Boss Rubel"),
            RuqaiyaFeature(6, "হোয়াটসঅ্যাপ লুপ ব্রডকাস্ট", "WhatsApp Loop Broadcast", "Core", "WhatsApp broadcast shuru", "Broadcasts messages with 3s anti-ban safety interval"),
            RuqaiyaFeature(7, "ইনফিনিক্স হট ১২ প্লে হার্ডওয়্যার টিউন", "Infinix HOT 12 Play Tuner", "Core", "Tune phone", "Allocates 4GB+3GB MemFusion and enforces 500MB free RAM pool"),
            RuqaiyaFeature(8, "বস রুবেল ফেস আনলক গার্ড", "Boss Rubel Face Guard", "Khata&Face", "Boss ke dekhao", "CameraX biometrics with 0.7f confidence threshold"),
            RuqaiyaFeature(9, "ফাইন্ড মাই ফোন অ্যান্টি-থেফট", "Find My Phone Anti-Theft", "Khata&Face", "Chor dhoro", "Visual toast at (0, -1100) and siren triggers"),
            RuqaiyaFeature(10, "খাতা হিসাব ও ধার ক্যাশ খতিয়ান", "Khata Hisab & Dhar Ledger", "Khata&Face", "Khatar hisab dao", "Stores local transactions in SQLite khata.db"),
            RuqaiyaFeature(11, "ফ্ল্যাশলাইট জিএক্সসি স্ট্রোব", "Flashlight GXC Strobe", "Khata&Face", "Flash jalo", "Multi-frequency camera torch strobe"),
            RuqaiyaFeature(12, "ইউনিভার্সাল ব্রেন মাল্টি-এআই", "Universal Brain Multi-AI", "Pro", "Brain ON", "Seamlessly connects to Gemini, OpenAI, Claude, Groq, DeepSeek, Mistral"),
            RuqaiyaFeature(13, "গড #১ সেলফ-আপডেট ল্যাব", "GOD #1 Self-Update Engine", "Pro", "Self update koro", "AI code generation, regex repair, in-app ./gradlew build, and FileProvider install"),
            RuqaiyaFeature(14, "ফ্লোটিং চিবি অবতার উইজেট", "Floating Chibi Avatar", "Pro", "Chibi asho", "Pinch-scalable 40-300dp companion with draggable coordinates"),
            RuqaiyaFeature(15, "রিয়েল-টাইম অ্যাক্টিভিটি ফিড", "Real-Time Activity Feed", "Pro", "Activity feed dekhao", "Chronological RecyclerView log of self-updates, AI calls & system status")
        )
    }
}
