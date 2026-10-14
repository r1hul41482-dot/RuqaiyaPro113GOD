package com.ruqaiyapro.debug.brain

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BrainManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "ruqaiya_brain_secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("ruqaiya_brain_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    fun getSelectedProvider(): AIProviderType {
        val name = prefs.getString("selected_provider", AIProviderType.GEMINI.name) ?: AIProviderType.GEMINI.name
        return try {
            AIProviderType.valueOf(name)
        } catch (e: Exception) {
            AIProviderType.GEMINI
        }
    }

    fun setSelectedProvider(provider: AIProviderType) {
        prefs.edit().putString("selected_provider", provider.name).apply()
    }

    fun getApiKey(provider: AIProviderType): String {
        return prefs.getString("api_key_${provider.name}", "") ?: ""
    }

    fun setApiKey(provider: AIProviderType, key: String) {
        prefs.edit().putString("api_key_${provider.name}", key.trim()).apply()
    }

    fun isBrainActive(): Boolean {
        val current = getSelectedProvider()
        return getApiKey(current).isNotBlank()
    }

    fun isDynamicFeaturesEnabled(): Boolean {
        return prefs.getBoolean("dynamic_ai_features_enabled", true)
    }

    fun setDynamicFeaturesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_ai_features_enabled", enabled).apply()
    }

    suspend fun generate(prompt: String, systemPrompt: String? = null): BrainResponse = withContext(Dispatchers.IO) {
        val provider = getSelectedProvider()
        val key = getApiKey(provider)
        val startTime = System.currentTimeMillis()

        if (key.isBlank()) {
            return@withContext BrainResponse(
                success = false,
                text = "BRAIN OFF: API key bosano hoyni. Settings tab e key din.",
                provider = provider,
                latencyMs = 0,
                error = "API Key is missing for ${provider.displayName}"
            )
        }

        try {
            val responseText = when (provider) {
                AIProviderType.GEMINI -> callGemini(key, prompt, systemPrompt)
                AIProviderType.OPENAI -> callOpenAI("https://api.openai.com/v1/chat/completions", key, "gpt-4o", prompt, systemPrompt)
                AIProviderType.GROQ -> callOpenAI("https://api.groq.com/openai/v1/chat/completions", key, "llama-3.1-70b-versatile", prompt, systemPrompt)
                AIProviderType.DEEPSEEK -> callOpenAI("https://api.deepseek.com/chat/completions", key, "deepseek-chat", prompt, systemPrompt)
                AIProviderType.MISTRAL -> callOpenAI("https://api.mistral.ai/v1/chat/completions", key, "mistral-large-latest", prompt, systemPrompt)
                AIProviderType.CLAUDE -> callClaude(key, prompt, systemPrompt)
            }
            val latency = System.currentTimeMillis() - startTime
            BrainResponse(success = true, text = responseText, provider = provider, latencyMs = latency)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            BrainResponse(success = false, text = "AI call failed: ${e.message}", provider = provider, latencyMs = latency, error = e.localizedMessage)
        }
    }

    private fun callGemini(apiKey: String, userPrompt: String, systemPrompt: String?): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val json = JSONObject()
        val contents = JSONArray()
        val parts = JSONArray()

        val fullText = if (!systemPrompt.isNullOrBlank()) "$systemPrompt\n\n$userPrompt" else userPrompt
        parts.put(JSONObject().put("text", fullText))
        contents.put(JSONObject().put("parts", parts))
        json.put("contents", contents)

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).post(body).build()

        client.newCall(req).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}: $respBody")
            val root = JSONObject(respBody)
            return root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private fun callOpenAI(endpoint: String, apiKey: String, model: String, userPrompt: String, systemPrompt: String?): String {
        val json = JSONObject().apply {
            put("model", model)
            val messages = JSONArray()
            if (!systemPrompt.isNullOrBlank()) {
                messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
            }
            messages.put(JSONObject().put("role", "user").put("content", userPrompt))
            put("messages", messages)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}: $respBody")
            val root = JSONObject(respBody)
            return root.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    private fun callClaude(apiKey: String, userPrompt: String, systemPrompt: String?): String {
        val json = JSONObject().apply {
            put("model", "claude-3-5-sonnet-20241022")
            put("max_tokens", 2048)
            if (!systemPrompt.isNullOrBlank()) put("system", systemPrompt)
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", userPrompt))
            put("messages", messages)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}: $respBody")
            val root = JSONObject(respBody)
            return root.getJSONArray("content").getJSONObject(0).getString("text")
        }
    }
}
