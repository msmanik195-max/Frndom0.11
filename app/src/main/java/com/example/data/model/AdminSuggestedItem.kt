package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties
import org.json.JSONObject

enum class SuggestedItemType(val label: String) {
    PROFILE("Profile"),
    PAGE("Page"),
    GROUP("Group")
}

@IgnoreExtraProperties
data class AdminSuggestedItem(
    val id: String = "",
    val type: SuggestedItemType = SuggestedItemType.PROFILE,
    val targetId: String = "", // uid, pageId, or groupId
    val title: String = "", // Name of profile, page, or group
    val subtitle: String = "", // Bio, category, followers, privacy
    val imageUrl: String = "", // Avatar / Logo / Cover
    val isVerified: Boolean = false,
    val badgeText: String = "", // e.g. "Featured", "Recommended", "Top Creator"
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "type" to type.name,
        "targetId" to targetId,
        "title" to title,
        "subtitle" to subtitle,
        "imageUrl" to imageUrl,
        "isVerified" to isVerified,
        "badgeText" to badgeText,
        "order" to order,
        "createdAt" to createdAt
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("targetId", targetId)
        put("title", title)
        put("subtitle", subtitle)
        put("imageUrl", imageUrl)
        put("isVerified", isVerified)
        put("badgeText", badgeText)
        put("order", order)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): AdminSuggestedItem {
            val typeStr = obj.optString("type", SuggestedItemType.PROFILE.name)
            val type = try {
                SuggestedItemType.valueOf(typeStr)
            } catch (_: Exception) {
                SuggestedItemType.PROFILE
            }
            return AdminSuggestedItem(
                id = obj.optString("id", ""),
                type = type,
                targetId = obj.optString("targetId", ""),
                title = obj.optString("title", ""),
                subtitle = obj.optString("subtitle", ""),
                imageUrl = obj.optString("imageUrl", ""),
                isVerified = obj.optBoolean("isVerified", false),
                badgeText = obj.optString("badgeText", ""),
                order = obj.optInt("order", 0),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}
