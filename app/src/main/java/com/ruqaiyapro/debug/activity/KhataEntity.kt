package com.ruqaiyapro.debug.activity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "khata")
data class KhataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val amount: Double,
    val iGave: Boolean, // true = ami dilam (se amar kache dhari), false = ami nilam (ami dhari)
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
