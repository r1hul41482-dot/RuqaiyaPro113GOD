package com.ruqaiyapro.debug.brain

// defaultModel can be changed from Settings (model names change over time).
enum class AIProviderType(val displayName: String, val hint: String, val defaultModel: String) {
    GEMINI("Gemini", "AIzaSy...", "gemini-2.5-flash"),
    OPENAI("GPT", "sk-proj-...", "gpt-4o"),
    CLAUDE("Claude", "sk-ant-...", "claude-sonnet-5-5"),
    GROQ("Groq", "gsk_...", "llama-3.3-70b-versatile"),
    DEEPSEEK("DeepSeek", "sk-...", "deepseek-chat"),
    MISTRAL("Mistral", "...", "mistral-large-latest")
}

data class BrainResponse(
    val success: Boolean,
    val text: String,
    val provider: AIProviderType,
    val latencyMs: Long,
    val error: String? = null
)
