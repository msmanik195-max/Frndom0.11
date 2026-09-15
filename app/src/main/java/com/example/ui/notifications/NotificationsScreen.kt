package com.example.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.NotificationItem
import com.example.data.model.ReactionType
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.LocalIsDarkMode

@Composable
fun NotificationsScreen(
    currentUserId: String,
    notificationRepository: NotificationRepository,
    userRepository: UserRepository,
    onUserClick: (UserProfile) -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode = LocalIsDarkMode.current
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val appSettings by adminRepo.appSettingsFlow.collectAsState()

    val notifications by notificationRepository.getNotificationsFlow(currentUserId).collectAsState(initial = emptyList())
    val allUsers by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())

    // Admin Panel Setting Enforcement:
    // If like & comment notifications are disabled by admin, filter out all like and comment notifications!
    val displayedNotifications = remember(notifications, appSettings.engagementNotificationsEnabled) {
        if (!appSettings.engagementNotificationsEnabled) {
            notifications.filter { it.type != "like" && it.type != "comment" }
        } else {
            notifications
        }
    }

    // When engagement notifications are disabled, also purge them from storage so they are completely removed
    LaunchedEffect(appSettings.engagementNotificationsEnabled, currentUserId) {
        if (!appSettings.engagementNotificationsEnabled) {
            notificationRepository.purgeEngagementNotifications(currentUserId)
            notificationRepository.purgeEngagementNotifications("global")
        }
    }

    val bgColor = if (isDarkMode) Color(0xFF18191A) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)
    val emptyIconBg = if (isDarkMode) Color(0xFF242526) else Color(0xFFF0F2F5)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("notifications_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("notifications_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Notifications",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (displayedNotifications.isNotEmpty()) {
                    IconButton(
                        onClick = { notificationRepository.markAllAsRead(currentUserId) },
                        modifier = Modifier.testTag("mark_all_read_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all as read",
                            tint = Color(0xFF1877F2),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

            if (displayedNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = emptyIconBg,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (!appSettings.engagementNotificationsEnabled) {
                                "Official notices and direct updates will appear here."
                            } else {
                                "Likes, comments and follows on your posts will appear here."
                            },
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(displayedNotifications, key = { it.id }) { item ->
                        val isAdminAnnouncement = item.type.startsWith("admin") ||
                                item.senderId == "admin" ||
                                item.senderName.contains("Frndom", ignoreCase = true)

                        if (isAdminAnnouncement) {
                            // Large, beautiful Announcement Card sent from Admin Panel
                            // No user profile, no ID, directly displays "Frndom টিম", not clickable to navigate away
                            AdminAnnouncementCard(
                                notification = item,
                                isDarkMode = isDarkMode
                            )
                        } else {
                            val targetSender = allUsers.find { it.uid == item.senderId } ?: UserProfile(
                                uid = item.senderId,
                                fullName = item.senderName,
                                profilePictureUrl = item.senderAvatarUrl
                            )

                            NotificationRowItem(
                                notification = item,
                                isDarkMode = isDarkMode,
                                onClick = { onUserClick(targetSender) },
                                onAvatarClick = { onUserClick(targetSender) }
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                    }
                }
            }
        }
    }
}

/**
 * Large, beautiful card for notifications sent from the Admin Panel.
 * Directly identifies as "Frndom টিম" with verified badge.
 * Displays title, description, and media image.
 * No user avatar, no user ID, and clicking will not navigate away.
 */
@Composable
private fun AdminAnnouncementCard(
    notification: NotificationItem,
    isDarkMode: Boolean
) {
    val cardBg = if (isDarkMode) Color(0xFF242526) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val timeAgo = remember(notification.timestamp) { formatTimeAgo(notification.timestamp) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("admin_announcement_card_${notification.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: "Frndom টিম" official badge + Time ago
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1877F2).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFF1877F2),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Frndom টিম",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1877F2)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Official",
                            tint = Color(0xFF1877F2),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = timeAgo,
                    fontSize = 12.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Announcement Title
            if (notification.title.isNotBlank()) {
                Text(
                    text = notification.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Announcement Description / Content
            if (notification.content.isNotBlank()) {
                Text(
                    text = notification.content,
                    fontSize = 14.5.sp,
                    color = if (notification.title.isNotBlank()) textSecondary else textPrimary,
                    lineHeight = 20.sp
                )
            }

            // Announcement Media Image
            if (notification.imageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = notification.imageUrl,
                    contentDescription = "Announcement Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun NotificationRowItem(
    notification: NotificationItem,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val backgroundColor = if (!notification.isRead) {
        if (isDarkMode) Color(0xFF1E2A3A) else Color(0xFFEBF5FF)
    } else {
        if (isDarkMode) Color(0xFF18191A) else Color.White
    }
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val timeAgo = remember(notification.timestamp) { formatTimeAgo(notification.timestamp) }

    val isLikeType = notification.type == "like" || notification.type == "reaction"
    val isCommentType = notification.type == "comment"

    // Resolve the exact reaction emoji
    val reactionType = ReactionType.fromKey(notification.reactionKey)
    val reactionEmoji = when {
        reactionType != null -> reactionType.emoji
        notification.content.contains("loved", ignoreCase = true) || notification.content.contains("❤️") -> "❤️"
        notification.content.contains("🥰") -> "🥰"
        notification.content.contains("😆") || notification.content.contains("haha", ignoreCase = true) -> "😆"
        notification.content.contains("😮") || notification.content.contains("wow", ignoreCase = true) -> "😮"
        notification.content.contains("😢") || notification.content.contains("sad", ignoreCase = true) -> "😢"
        notification.content.contains("😡") || notification.content.contains("angry", ignoreCase = true) -> "😡"
        else -> "👍"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("notification_item_${notification.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with Type Icon Badge
        Box(modifier = Modifier.size(54.dp)) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (notification.senderAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = notification.senderAvatarUrl,
                        contentDescription = notification.senderName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = notification.senderName.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Action Badge: Distinctive comment icon for comments, exact emoji for likes/reactions
            Surface(
                shape = CircleShape,
                color = when {
                    isLikeType -> Color.White
                    isCommentType -> Color(0xFF1877F2)
                    notification.type == "follow" -> Color(0xFF2E7D32)
                    else -> Color(0xFF1877F2)
                },
                border = if (isLikeType) BorderStroke(1.dp, Color(0xFFE4E6EB)) else null,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    when {
                        isLikeType -> {
                            Text(
                                text = reactionEmoji,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        isCommentType -> {
                            Icon(
                                imageVector = Icons.Filled.ChatBubble,
                                contentDescription = "Comment",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        notification.type == "follow" -> {
                            Icon(
                                imageVector = Icons.Filled.PersonAdd,
                                contentDescription = "Follow",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        notification.type == "friend_request" || notification.type == "friend_accept" -> {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Friend",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Text & Timestamp
        Column(modifier = Modifier.weight(1f)) {
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)) {
                    append(notification.senderName)
                }
                append(" ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, color = textPrimary, fontSize = 14.sp)) {
                    append(notification.content)
                }
            }

            Text(
                text = annotatedString,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = timeAgo,
                fontSize = 12.sp,
                color = if (!notification.isRead) Color(0xFF1877F2) else textSecondary,
                fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF1877F2), CircleShape)
            )
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
