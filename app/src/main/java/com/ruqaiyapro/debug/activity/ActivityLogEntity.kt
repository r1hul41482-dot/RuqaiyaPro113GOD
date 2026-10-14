package com.ruqaiyapro.debug.activity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // SELF_UPDATE, AI_GENERATION, SYSTEM_STATUS, FEEDBACK
    val title: String,
    val description: String,
    val status: String, // SUCCESS, FAILED, INFO, WARNING
    val providerName: String? = null,
    val latencyMs: Long? = null
)
