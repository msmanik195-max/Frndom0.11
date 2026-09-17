package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class PostReport(
    val id: String = "",
    val postId: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending", // "pending", "resolved", "dismissed"
    val postAuthorId: String = "",
    val postAuthorName: String = "",
    val postAuthorAvatarUrl: String = "",
    val postContent: String = "",
    val postMediaType: String = "text",
    val postMediaUrl: String = "",
    val postMediaUrls: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "postId" to postId,
            "reporterId" to reporterId,
            "reporterName" to reporterName,
            "reason" to reason,
            "details" to details,
            "timestamp" to timestamp,
            "status" to status,
            "postAuthorId" to postAuthorId,
            "postAuthorName" to postAuthorName,
            "postAuthorAvatarUrl" to postAuthorAvatarUrl,
            "postContent" to postContent,
            "postMediaType" to postMediaType,
            "postMediaUrl" to postMediaUrl,
            "postMediaUrls" to postMediaUrls
        )
    }
}
