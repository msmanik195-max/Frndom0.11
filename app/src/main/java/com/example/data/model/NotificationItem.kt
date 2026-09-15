package com.example.data.model

data class NotificationItem(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "User",
    val senderAvatarUrl: String = "",
    val postId: String = "",
    val type: String = "like", // "like", "comment", "follow", "friend_request", "friend_accept", "admin_announcement", "admin_notice"
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val reactionKey: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "recipientId" to recipientId,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderAvatarUrl" to senderAvatarUrl,
        "postId" to postId,
        "type" to type,
        "title" to title,
        "content" to content,
        "imageUrl" to imageUrl,
        "timestamp" to timestamp,
        "isRead" to isRead,
        "reactionKey" to reactionKey
    )
}
