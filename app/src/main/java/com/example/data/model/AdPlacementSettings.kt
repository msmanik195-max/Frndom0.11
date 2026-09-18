package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties
import org.json.JSONObject

/**
 * Stores placement rules for sponsored advertisements:
 * - homeAdsEnabled: whether sponsored ads appear in Home feed
 * - homePostInterval: after how many organic posts a sponsored ad appears (default: 5)
 * - reelsAdsEnabled: whether sponsored video ads appear in Reels feed
 * - reelsVideoInterval: after how many organic reels a sponsored video ad appears (default: 5)
 */
@IgnoreExtraProperties
data class AdPlacementSettings(
    val homeAdsEnabled: Boolean = true,
    val homePostInterval: Int = 5,
    val homeVideoAdsEnabled: Boolean = true,
    val reelsAdsEnabled: Boolean = true,
    val reelsVideoInterval: Int = 5,
    val reelsImageAdsEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "homeAdsEnabled" to homeAdsEnabled,
        "homePostInterval" to homePostInterval,
        "homeVideoAdsEnabled" to homeVideoAdsEnabled,
        "reelsAdsEnabled" to reelsAdsEnabled,
        "reelsVideoInterval" to reelsVideoInterval,
        "reelsImageAdsEnabled" to reelsImageAdsEnabled,
        "updatedAt" to updatedAt
    )

    fun toJsonString(): String {
        val obj = JSONObject()
        obj.put("homeAdsEnabled", homeAdsEnabled)
        obj.put("homePostInterval", homePostInterval)
        obj.put("homeVideoAdsEnabled", homeVideoAdsEnabled)
        obj.put("reelsAdsEnabled", reelsAdsEnabled)
        obj.put("reelsVideoInterval", reelsVideoInterval)
        obj.put("reelsImageAdsEnabled", reelsImageAdsEnabled)
        obj.put("updatedAt", updatedAt)
        return obj.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String?): AdPlacementSettings {
            if (jsonStr.isNullOrBlank()) return AdPlacementSettings()
            return try {
                val obj = JSONObject(jsonStr)
                AdPlacementSettings(
                    homeAdsEnabled = obj.optBoolean("homeAdsEnabled", true),
                    homePostInterval = obj.optInt("homePostInterval", 5).coerceAtLeast(1),
                    homeVideoAdsEnabled = obj.optBoolean("homeVideoAdsEnabled", true),
                    reelsAdsEnabled = obj.optBoolean("reelsAdsEnabled", true),
                    reelsVideoInterval = obj.optInt("reelsVideoInterval", 5).coerceAtLeast(1),
                    reelsImageAdsEnabled = obj.optBoolean("reelsImageAdsEnabled", true),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                AdPlacementSettings()
            }
        }
    }
}
