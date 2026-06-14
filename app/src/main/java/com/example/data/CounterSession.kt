package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class CounterSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val checkCount: Int = 0,
    val crossCount: Int = 0,
    val totalCheckTimeMs: Long = 0L,
    val totalCrossTimeMs: Long = 0L,
    val note: String = ""
) {
    val accuracy: Float
        get() {
            val total = checkCount + crossCount
            return if (total == 0) 0f else (checkCount.toFloat() / total.toFloat()) * 100f
        }

    val avgCheckTimeMs: Long
        get() = if (checkCount == 0) 0L else totalCheckTimeMs / checkCount

    val avgCrossTimeMs: Long
        get() = if (crossCount == 0) 0L else totalCrossTimeMs / crossCount
}
