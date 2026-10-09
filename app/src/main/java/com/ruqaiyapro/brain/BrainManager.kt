package com.ruqaiyapro.brain

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AIProvider(val displayName: String, val defaultModel: String) {
    GEMINI("Google Gemini 1.5 Flash", "gemini-1.5-flash"),
    OPENAI("OpenAI ChatGPT 4o", "gpt-4o"),
    CLAUDE("Anthropic Claude 3.5 Sonnet", "claude-3-5-sonnet-20241022"),
    GROQ("Groq Llama 3.3 70B", "llama-3.3-70b-versatile"),
    DEEPSEEK("DeepSeek V3", "deepseek-chat"),
    MISTRAL("Mistral Large", "mistral-large-latest")
}

class BrainManager private constructor(context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "ruqaiya_brain_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        @Volatile
        private var instance: BrainManager? = null

        fun getInstance(context: Context): BrainManager =
            instance ?: synchronized(this) {
                instance ?: BrainManager(context.applicationContext).also { instance = it }
            }
    }

    fun saveProviderConfig(provider: AIProvider, apiKey: String, dynamicEnabled: Boolean) {
        encryptedPrefs.edit()
            .putString("ai_provider", provider.name)
            .putString("api_key", apiKey.trim())
            .putBoolean("dynamic_enabled", dynamicEnabled)
            .apply()
    }

    fun getSelectedProvider(): AIProvider {
        val name = encryptedPrefs.getString("ai_provider", AIProvider.GEMINI.name) ?: AIProvider.GEMINI.name
        return try { AIProvider.valueOf(name) } catch (e: Exception) { AIProvider.GEMINI }
    }

    fun getApiKey(): String = encryptedPrefs.getString("api_key", "") ?: ""

    fun isDynamicEnabled(): Boolean = encryptedPrefs.getBoolean("dynamic_enabled", true)

    fun isBrainActive(): Boolean = getApiKey().isNotEmpty()

    /**
     * Universal Dispatcher: executes prompt on ANY configured AI provider
     */
    suspend fun askUniversalBrain(prompt: String, systemPrompt: String = DEFAULT_SYSTEM_PROMPT): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No API key configured. Input ANY AI API Key in Settings to turn Brain ON!"))
        }

        val provider = getSelectedProvider()
        try {
            val responseText = when (provider) {
                AIProvider.GEMINI -> callGemini(apiKey, prompt, systemPrompt)
                AIProvider.OPENAI -> callOpenAI(apiKey, prompt, systemPrompt, "https://api.openai.com/v1/chat/completions", provider.defaultModel)
                AIProvider.CLAUDE -> callClaude(apiKey, prompt, systemPrompt)
                AIProvider.GROQ -> callOpenAI(apiKey, prompt, systemPrompt, "https://api.groq.com/openai/v1/chat/completions", provider.defaultModel)
                AIProvider.DEEPSEEK -> callOpenAI(apiKey, prompt, systemPrompt, "https://api.deepseek.com/chat/completions", provider.defaultModel)
                AIProvider.MISTRAL -> callOpenAI(apiKey, prompt, systemPrompt, "https://api.mistral.ai/v1/chat/completions", provider.defaultModel)
            }
            Result.success(responseText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callGemini(apiKey: String, prompt: String, systemPrompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val bodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemPrompt\n\nUser query: $prompt"))
                    })
                })
            })
        }
        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        val raw = response.body?.string() ?: ""
        val json = JSONObject(raw)
        return json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }

    private fun callOpenAI(apiKey: String, prompt: String, systemPrompt: String, endpoint: String, model: String): String {
        val bodyJson = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", prompt))
            })
        }
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        val raw = response.body?.string() ?: ""
        val json = JSONObject(raw)
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    private fun callClaude(apiKey: String, prompt: String, systemPrompt: String): String {
        val url = "https://api.anthropic.com/v1/messages"
        val bodyJson = JSONObject().apply {
            put("model", "claude-3-5-sonnet-20241022")
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "user").put("content", prompt))
            })
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        val raw = response.body?.string() ?: ""
        val json = JSONObject(raw)
        return json.getJSONArray("content").getJSONObject(0).getString("text")
    }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = 
            "You are Ruqaiya, the beloved smart virtual companion of Boss Rubel. " +
            "Speak warmly in sweet Bengali/English. Always recognize Boss Rubel as your respected master and king. " +
            "Be witty, caring, and ready to assist with WhatsApp, phone controls, shayari, and funny roasts!"
    }
}