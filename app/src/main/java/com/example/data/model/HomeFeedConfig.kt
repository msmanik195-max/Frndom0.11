package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents the customizable blocks/sections displayed in the user's Home Feed.
 * Admin can reorder, duplicate, adjust item batch ranges (e.g. 2-5 images, 1-2 videos),
 * and toggle "Friends Only" filtering.
 */
@IgnoreExtraProperties
data class HomeFeedConfig(
    val onlyFriendsPosts: Boolean = false, // When true, only friends' stories, images & videos appear on Home Feed (Reels page still shows everyone)
    val sections: List<HomeFeedSection> = defaultSections(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "onlyFriendsPosts" to onlyFriendsPosts,
        "sections" to sections.map { it.toMap() },
        "updatedAt" to updatedAt
    )

    fun toJsonString(): String {
        val root = JSONObject()
        root.put("onlyFriendsPosts", onlyFriendsPosts)
        root.put("updatedAt", updatedAt)
        val arr = JSONArray()
        sections.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("type", s.type.name)
            obj.put("title", s.title)
            obj.put("enabled", s.enabled)
            obj.put("minCount", s.minCount)
            obj.put("maxCount", s.maxCount)
            arr.put(obj)
        }
        root.put("sections", arr)
        return root.toString()
    }

    companion object {
        fun defaultSections(): List<HomeFeedSection> = listOf(
            HomeFeedSection(
                id = "sec_stories_1",
                type = FeedSectionType.STORIES,
                title = "Stories Tray",
                enabled = true,
                minCount = 1,
                maxCount = 1
            ),
            HomeFeedSection(
                id = "sec_images_1",
                type = FeedSectionType.IMAGE_POSTS,
                title = "Photo / Image Posts",
                enabled = true,
                minCount = 2,
                maxCount = 5
            ),
            HomeFeedSection(
                id = "sec_friends_1",
                type = FeedSectionType.FRIEND_SUGGESTIONS,
                title = "Friend Suggestions (Add Friend)",
                enabled = true,
                minCount = 3,
                maxCount = 6
            ),
            HomeFeedSection(
                id = "sec_videos_1",
                type = FeedSectionType.VIDEO_POSTS,
                title = "Video Posts",
                enabled = true,
                minCount = 1,
                maxCount = 2
            )
        )

        fun fromJsonString(jsonStr: String?): HomeFeedConfig {
            if (jsonStr.isNullOrBlank()) return HomeFeedConfig()
            return try {
                val obj = JSONObject(jsonStr)
                val friendsOnly = obj.optBoolean("onlyFriendsPosts", false)
                val updated = obj.optLong("updatedAt", System.currentTimeMillis())
                val arr = obj.optJSONArray("sections")
                val list = mutableListOf<HomeFeedSection>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val typeStr = item.optString("type", FeedSectionType.IMAGE_POSTS.name)
                        val type = try {
                            FeedSectionType.valueOf(typeStr)
                        } catch (_: Exception) {
                            FeedSectionType.IMAGE_POSTS
                        }
                        list.add(
                            HomeFeedSection(
                                id = item.optString("id", "sec_$i"),
                                type = type,
                                title = item.optString("title", type.defaultTitle),
                                enabled = item.optBoolean("enabled", true),
                                minCount = item.optInt("minCount", 1),
                                maxCount = item.optInt("maxCount", 5)
                            )
                        )
                    }
                }
                HomeFeedConfig(
                    onlyFriendsPosts = friendsOnly,
                    sections = if (list.isEmpty()) defaultSections() else list,
                    updatedAt = updated
                )
            } catch (_: Exception) {
                HomeFeedConfig()
            }
        }
    }
}

enum class FeedSectionType(val defaultTitle: String) {
    STORIES("Stories Tray"),
    IMAGE_POSTS("Photo / Image Posts"),
    VIDEO_POSTS("Video Posts"),
    FRIEND_SUGGESTIONS("Friend Suggestions (Add Friend)"),
    TEXT_POSTS("Text Posts")
}

@IgnoreExtraProperties
data class HomeFeedSection(
    val id: String = "",
    val type: FeedSectionType = FeedSectionType.IMAGE_POSTS,
    val title: String = "",
    val enabled: Boolean = true,
    val minCount: Int = 2,
    val maxCount: Int = 5
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "type" to type.name,
        "title" to title,
        "enabled" to enabled,
        "minCount" to minCount,
        "maxCount" to maxCount
    )
}
