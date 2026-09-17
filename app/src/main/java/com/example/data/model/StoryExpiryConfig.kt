package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties
import org.json.JSONObject

@IgnoreExtraProperties
data class StoryExpiryConfig(
    val hours: Int = 24,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculates total duration in milliseconds. Minimum duration 5 seconds to prevent negative or 0 loops.
     */
    val totalDurationMs: Long
        get() {
            val totalSeconds = (hours.toLong() * 3600L) + (minutes.toLong() * 60L) + seconds.toLong()
            return (if (totalSeconds <= 0L) 86400L else totalSeconds) * 1000L
        }

    val formattedSummary: String
        get() {
            val parts = mutableListOf<String>()
            if (hours > 0) parts.add("$hours hr")
            if (minutes > 0) parts.add("$minutes min")
            if (seconds > 0) parts.add("$seconds sec")
            if (parts.isEmpty()) parts.add("24 hr")
            return parts.joinToString(" ")
        }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "hours" to hours,
            "minutes" to minutes,
            "seconds" to seconds,
            "totalDurationMs" to totalDurationMs,
            "updatedAt" to updatedAt
        )
    }

    fun toJsonString(): String {
        return JSONObject().apply {
            put("hours", hours)
            put("minutes", minutes)
            put("seconds", seconds)
            put("updatedAt", updatedAt)
        }.toString()
    }

    companion object {
        fun fromJsonString(json: String?): StoryExpiryConfig {
            if (json.isNullOrBlank()) return StoryExpiryConfig()
            return try {
                val obj = JSONObject(json)
                StoryExpiryConfig(
                    hours = obj.optInt("hours", 24),
                    minutes = obj.optInt("minutes", 0),
                    seconds = obj.optInt("seconds", 0),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                StoryExpiryConfig()
            }
        }
    }
}
