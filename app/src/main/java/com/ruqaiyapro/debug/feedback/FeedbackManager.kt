package com.ruqaiyapro.debug.feedback

import android.content.Context
import com.ruqaiyapro.debug.activity.RuqaiyaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedbackManager(private val context: Context) {

    private val db = RuqaiyaDatabase.getDatabase(context)

    suspend fun recordFeedback(prompt: String, response: String, provider: String, isThumbsUp: Boolean, tag: String? = null) {
        withContext(Dispatchers.IO) {
            val rating = if (isThumbsUp) 1 else -1
            db.feedbackDao().insertFeedback(
                FeedbackEntity(
                    userPrompt = prompt,
                    aiResponse = response,
                    provider = provider,
                    rating = rating,
                    tag = tag
                )
            )
        }
    }

    suspend fun getPromptContextPrefix(): String = withContext(Dispatchers.IO) {
        val liked = db.feedbackDao().getTopLikedPrompts()
        val disliked = db.feedbackDao().getDislikedPrompts()

        val sb = StringBuilder()
        if (liked.isNotEmpty()) {
            sb.append("\nUser Preferences (Boss Rubel liked these styles):\n")
            liked.take(3).forEach {
                sb.append("- Prompt: '${it.userPrompt}' -> Quality high\n")
            }
        }
        if (disliked.isNotEmpty()) {
            sb.append("\nAvoid these errors (Boss Rubel disliked):\n")
            disliked.take(3).forEach {
                sb.append("- Avoid repeating: '${it.aiResponse.take(80)}...' for '${it.userPrompt}'\n")
            }
        }
        sb.toString()
    }
}
