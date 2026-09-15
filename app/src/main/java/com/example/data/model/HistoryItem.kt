package com.example.data.model

data class HistoryItem(
    val id: String = "",
    val itemId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val mediaType: String = "post", // "video", "reel", "photo", "image", "story", "post"
    val mediaUrl: String = "",
    val mediaUrls: List<String> = emptyList(),
    val isStory: Boolean = false,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val viewedAt: Long = System.currentTimeMillis()
) {
    fun toPostItem(): PostItem {
        return PostItem(
            id = itemId.ifBlank { id },
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            content = content,
            mediaType = when (mediaType) {
                "story" -> "photo"
                "image" -> "photo"
                else -> mediaType
            },
            mediaUrl = mediaUrl,
            mediaUrls = mediaUrls,
            likesCount = likesCount,
            commentsCount = commentsCount,
            createdAt = createdAt
        )
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "itemId" to itemId,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorAvatarUrl" to authorAvatarUrl,
            "content" to content,
            "mediaType" to mediaType,
            "mediaUrl" to mediaUrl,
            "mediaUrls" to mediaUrls,
            "isStory" to isStory,
            "likesCount" to likesCount,
            "commentsCount" to commentsCount,
            "createdAt" to createdAt,
            "viewedAt" to viewedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): HistoryItem {
            @Suppress("UNCHECKED_CAST")
            val rawMediaUrls = map["mediaUrls"] as? List<*> ?: emptyList<Any>()
            val urls = rawMediaUrls.mapNotNull { it?.toString() }
            return HistoryItem(
                id = map["id"]?.toString() ?: "",
                itemId = map["itemId"]?.toString() ?: "",
                authorId = map["authorId"]?.toString() ?: "",
                authorName = map["authorName"]?.toString() ?: "",
                authorAvatarUrl = map["authorAvatarUrl"]?.toString() ?: "",
                content = map["content"]?.toString() ?: "",
                mediaType = map["mediaType"]?.toString() ?: "post",
                mediaUrl = map["mediaUrl"]?.toString() ?: "",
                mediaUrls = urls,
                isStory = map["isStory"] as? Boolean ?: false,
                likesCount = (map["likesCount"] as? Number)?.toInt() ?: 0,
                commentsCount = (map["commentsCount"] as? Number)?.toInt() ?: 0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                viewedAt = (map["viewedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
