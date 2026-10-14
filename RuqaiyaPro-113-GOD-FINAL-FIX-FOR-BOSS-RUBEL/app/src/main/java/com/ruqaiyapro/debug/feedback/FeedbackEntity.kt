package com.ruqaiyapro.debug.feedback

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_feedbacks")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userPrompt: String,
    val aiResponse: String,
    val provider: String,
    val rating: Int, // 1 for Thumbs Up, -1 for Thumbs Down
    val tag: String? = null // e.g. "SHAYARI", "ROAST", "SELF_UPDATE"
)
