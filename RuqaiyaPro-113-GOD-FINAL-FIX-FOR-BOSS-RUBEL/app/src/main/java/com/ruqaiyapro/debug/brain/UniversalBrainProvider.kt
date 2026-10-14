package com.ruqaiyapro.debug.brain

enum class AIProviderType(val displayName: String, val hint: String, val defaultModel: String) {
    GEMINI("Gemini 1.5 Flash", "AIzaSy...", "gemini-1.5-flash"),
    OPENAI("GPT-4o", "sk-proj-...", "gpt-4o"),
    CLAUDE("Claude 3.5 Sonnet", "sk-ant-...", "claude-3-5-sonnet-20241022"),
    GROQ("Groq Llama 3", "gsk_...", "llama-3.1-70b-versatile"),
    DEEPSEEK("DeepSeek V3", "sk-...", "deepseek-chat"),
    MISTRAL("Mistral Large", "...", "mistral-large-latest")
}

data class BrainResponse(
    val success: Boolean,
    val text: String,
    val provider: AIProviderType,
    val latencyMs: Long,
    val error: String? = null
)
