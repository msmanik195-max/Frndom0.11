package com.example.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.HistoryItem
import com.example.data.model.PostItem
import com.example.data.repository.WatchHistoryRepository
import com.example.ui.theme.LocalIsDarkMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WatchHistoryView(
    watchHistoryRepository: WatchHistoryRepository,
    onPostClick: (PostItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history by watchHistoryRepository.historyFlow.collectAsState()

    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)
    val chipSelectedBg = if (isDarkMode) Color(0xFF263951) else Color(0xFFE7F3FF)
    val chipSelectedContent = if (isDarkMode) Color(0xFF4599FF) else Color(0xFF1877F2)

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recent, 1: Lifetime
    val tabs = listOf("Recent", "Lifetime")

    var selectedTypeFilter by remember { mutableStateOf("All") } // "All", "Videos", "Images", "Posts"
    val typeFilters = listOf("All", "Videos", "Images", "Posts")

    var showClearDialog by remember { mutableStateOf(false) }

    // Recent cutoff: last 48 hours or top 25
    val fortyEightHoursAgo = System.currentTimeMillis() - (48 * 60 * 60 * 1000L)
    val tabFilteredHistory = remember(history, selectedTab) {
        if (selectedTab == 0) {
            val recentByTime = history.filter { it.viewedAt >= fortyEightHoursAgo }
            if (recentByTime.size < 15 && history.isNotEmpty()) {
                history.take(25)
            } else {
                recentByTime
            }
        } else {
            history
        }
    }

    val displayedHistory = remember(tabFilteredHistory, selectedTypeFilter) {
        when (selectedTypeFilter) {
            "Videos" -> tabFilteredHistory.filter { it.mediaType == "video" || it.mediaType == "reel" }
            "Images" -> tabFilteredHistory.filter { it.mediaType == "image" || it.mediaType == "photo" || it.mediaType == "story" }
            "Posts" -> tabFilteredHistory.filter { it.mediaType == "post" || it.mediaType == "text" }
            else -> tabFilteredHistory
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = bgCard,
            title = {
                Text(
                    text = "Clear History?",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will remove all viewed posts, videos, and images from your history across all devices. This cannot be undone.",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        watchHistoryRepository.clearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgScreen)
            .testTag("watch_history_view")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgCard)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                if (history.isNotEmpty()) {
                    TextButton(onClick = { showClearDialog = true }) {
                        Text(text = "Clear All", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tabs: Recent vs Lifetime
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = bgCard,
                contentColor = Color(0xFF1877F2),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF1877F2),
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp,
                                color = if (selectedTab == index) (if (isDarkMode) Color(0xFF4599FF) else Color(0xFF1877F2)) else textSecondary
                            )
                        }
                    )
                }
            }

            // Filter Chips (All, Videos, Images, Posts)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(typeFilters) { filterName ->
                    val isSelected = selectedTypeFilter == filterName
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = filterName },
                        label = {
                            Text(
                                text = filterName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5),
                            labelColor = textSecondary,
                            selectedContainerColor = chipSelectedBg,
                            selectedLabelColor = chipSelectedContent
                        ),
                        border = null,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Divider(thickness = 0.5.dp, color = dividerColor)

            if (displayedHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedTypeFilter == "Videos") Icons.Outlined.VideoLibrary else Icons.Default.History,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (selectedTab == 0) "No Recent History" else "No Lifetime History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedTab == 0)
                                "Videos, images, and posts you view from other users will appear here."
                            else
                                "Your complete history of posts, videos, and images you have viewed will be stored here.",
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedHistory, key = { it.id }) { item ->
                        HistoryCardItem(
                            item = item,
                            isDarkMode = isDarkMode,
                            bgCard = bgCard,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onItemClick = { onPostClick(item.toPostItem()) },
                            onRemoveClick = { watchHistoryRepository.removeItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCardItem(
    item: HistoryItem,
    isDarkMode: Boolean,
    bgCard: Color,
    textPrimary: Color,
    textSecondary: Color,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Media Preview Thumbnail
            Box(
                modifier = Modifier
                    .size(90.dp, 75.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)),
                contentAlignment = Alignment.Center
            ) {
                val previewUrl = item.mediaUrl.ifBlank { item.mediaUrls.firstOrNull().orEmpty() }
                if (previewUrl.isNotBlank()) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (item.mediaType == "video" || item.mediaType == "reel") {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else if (item.mediaType == "image" || item.mediaType == "photo" || item.mediaType == "story") {
                    if (previewUrl.isBlank()) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    if (previewUrl.isBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.Article,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Author row with avatar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.authorAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = item.authorAvatarUrl,
                            contentDescription = item.authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.authorName.ifBlank { "User" },
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color(0xFF4599FF) else Color(0xFF1877F2),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Content type badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = when (item.mediaType) {
                                "video", "reel" -> "Video"
                                "image", "photo" -> "Photo"
                                "story" -> "Story"
                                else -> "Post"
                            },
                            fontSize = 10.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.content.ifBlank {
                        when (item.mediaType) {
                            "video", "reel" -> "Video by ${item.authorName}"
                            "story" -> "Story by ${item.authorName}"
                            "image", "photo" -> "Photo post by ${item.authorName}"
                            else -> "Post by ${item.authorName}"
                        }
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val viewedText = formatRelativeTime(item.viewedAt)
                Text(
                    text = "Viewed $viewedText",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            // Remove button for this item
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timestamp))
    }
}
