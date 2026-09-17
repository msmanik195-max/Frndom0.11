package com.example.ui.menu

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PageItem
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.GroupPageRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.LocalIsDarkMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Unified Leaderboard Entry representing either a User Profile or a Page
 */
sealed class LeaderboardEntry {
    abstract val id: String
    abstract val name: String
    abstract val avatarUrl: String
    abstract val isVerified: Boolean
    abstract val totalLikes: Int
    abstract val totalComments: Int
    abstract val followersCount: Int
    abstract val totalScore: Double
    abstract val isPage: Boolean
    abstract val userProfile: UserProfile?
    abstract val pageItem: PageItem?

    data class ProfileEntry(
        val user: UserProfile,
        override val totalLikes: Int,
        override val totalComments: Int,
        override val followersCount: Int,
        override val totalScore: Double
    ) : LeaderboardEntry() {
        override val id: String get() = user.uid
        override val name: String get() = user.fullName.ifBlank { "${user.firstName} ${user.lastName}".trim() }.ifBlank { "User" }
        override val avatarUrl: String get() = user.profilePictureUrl
        override val isVerified: Boolean get() = user.isVerificationActive()
        override val isPage: Boolean get() = false
        override val userProfile: UserProfile get() = user
        override val pageItem: PageItem? get() = null
    }

    data class PageEntry(
        val page: PageItem,
        override val totalLikes: Int,
        override val totalComments: Int,
        override val followersCount: Int,
        override val totalScore: Double
    ) : LeaderboardEntry() {
        override val id: String get() = page.id
        override val name: String get() = page.name.ifBlank { "Page" }
        override val avatarUrl: String get() = page.avatarUrl.ifBlank { page.coverUrl }
        override val isVerified: Boolean get() = page.isBadgeActive()
        override val isPage: Boolean get() = true
        override val userProfile: UserProfile? get() = null
        override val pageItem: PageItem get() = page
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    userRepository: UserRepository,
    postRepository: PostRepository,
    onBack: () -> Unit,
    onUserClick: (UserProfile) -> Unit,
    groupPageRepository: GroupPageRepository? = null,
    adminRequestRepository: AdminRequestRepository? = null,
    onPageClick: (PageItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pageRepo = groupPageRepository ?: remember { GroupPageRepository.getInstance(context) }
    val adminRepo = adminRequestRepository ?: remember { AdminRequestRepository.getInstance(context) }

    val allUsers by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())
    val allPosts by postRepository.postsFlow.collectAsState()
    val allPages by pageRepo.pagesFlow.collectAsState()
    val appSettings by adminRepo.appSettingsFlow.collectAsState()

    val leaderboardLimit = appSettings.leaderboardLimit.coerceAtLeast(3)

    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)

    // Calculate metrics and aggregate for both Users and Pages
    val combinedLeaderboard = remember(allUsers, allPosts, allPages, leaderboardLimit) {
        val likesPerAuthor = mutableMapOf<String, Int>()
        val commentsPerAuthor = mutableMapOf<String, Int>()

        for (post in allPosts) {
            val authorId = post.authorId
            if (authorId.isNotBlank()) {
                likesPerAuthor[authorId] = (likesPerAuthor[authorId] ?: 0) + post.likesCount
                commentsPerAuthor[authorId] = (commentsPerAuthor[authorId] ?: 0) + post.commentsCount
            }
        }

        // 1. User Profiles
        val userEntries = allUsers.filter { it.uid.isNotBlank() }.map { u ->
            val likes = likesPerAuthor[u.uid] ?: 0
            val comments = commentsPerAuthor[u.uid] ?: 0
            val followers = if (u.friendsCount > 0) u.friendsCount else u.friendsMap.size
            val income = u.walletBalance.coerceAtLeast(0.0)

            // Comprehensive Activity & Engagement Score
            val score = (likes * 2.0) + (comments * 3.0) + (followers * 5.0) + (income * 10.0)

            LeaderboardEntry.ProfileEntry(
                user = u,
                totalLikes = likes,
                totalComments = comments,
                followersCount = followers,
                totalScore = score
            )
        }

        // 2. Pages
        val pageEntries = allPages.filter { it.id.isNotBlank() }.map { p ->
            // Likes on posts by page + page followers
            val pagePostLikes = (likesPerAuthor[p.id] ?: 0) + (likesPerAuthor["page_profile_${p.id}"] ?: 0)
            val pagePostComments = (commentsPerAuthor[p.id] ?: 0) + (commentsPerAuthor["page_profile_${p.id}"] ?: 0)
            val totalLikes = pagePostLikes + p.likesCount
            val followers = p.followersCount

            // Activity & Engagement Score for Pages
            val score = (totalLikes * 2.0) + (pagePostComments * 3.0) + (followers * 5.0)

            LeaderboardEntry.PageEntry(
                page = p,
                totalLikes = totalLikes,
                totalComments = pagePostComments,
                followersCount = followers,
                totalScore = score
            )
        }

        // Combine, sort descending by total activity score, take admin-configured limit (default 20)
        (userEntries + pageEntries)
            .sortedByDescending { it.totalScore }
            .take(leaderboardLimit)
    }

    val top3 = combinedLeaderboard.take(3)
    val remainingItems = if (combinedLeaderboard.size > 3) combinedLeaderboard.drop(3) else emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgScreen)
            .testTag("leaderboard_screen")
    ) {
        // Clean Minimalist Top App Bar (No Tabs)
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Leaderboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Top ${combinedLeaderboard.size} • Profiles & Pages",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("leaderboard_back_btn")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = bgCard)
        )

        // Leaderboard Content
        if (combinedLeaderboard.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFB300).copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Leaderboard Data Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Start posting, liking, commenting, and gaining followers to top the leaderboard!",
                        fontSize = 13.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TOP 3 PODIUM (Prominently displayed at the top)
                item {
                    PodiumBanner(
                        topEntries = top3,
                        onEntryClick = { entry ->
                            when (entry) {
                                is LeaderboardEntry.ProfileEntry -> onUserClick(entry.user)
                                is LeaderboardEntry.PageEntry -> onPageClick(entry.page)
                            }
                        },
                        isDarkMode = isDarkMode
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Section Header for remaining rankings
                if (remainingItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rankings (#4 - #${combinedLeaderboard.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "${remainingItems.size} remaining",
                                fontSize = 12.sp,
                                color = Color(0xFF1877F2),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Remaining items (#4 to limit, e.g. 17 items if default 20)
                    itemsIndexed(remainingItems, key = { _, item -> item.id }) { index, entry ->
                        val rank = index + 4
                        LeaderboardCard(
                            rank = rank,
                            entry = entry,
                            bgCard = bgCard,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDarkMode = isDarkMode,
                            onClick = {
                                when (entry) {
                                    is LeaderboardEntry.ProfileEntry -> onUserClick(entry.user)
                                    is LeaderboardEntry.PageEntry -> onPageClick(entry.page)
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

/**
 * Top 3 Podium Banner: 2nd place on left, 1st place in center (Gold), 3rd place on right
 */
@Composable
private fun PodiumBanner(
    topEntries: List<LeaderboardEntry>,
    onEntryClick: (LeaderboardEntry) -> Unit,
    isDarkMode: Boolean
) {
    if (topEntries.isEmpty()) return

    val first = topEntries.getOrNull(0)
    val second = topEntries.getOrNull(1)
    val third = topEntries.getOrNull(2)

    val bannerGradient = if (isDarkMode) {
        Brush.verticalGradient(listOf(Color(0xFF2A2312), Color(0xFF1E1C16)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFF9E6), Color(0xFFFFF0B8)))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_podium_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerGradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Top Achievers",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFFD48806)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 2nd Place (Silver)
                    if (second != null) {
                        PodiumSpot(
                            entry = second,
                            rank = 2,
                            medalColor = Color(0xFF90A4AE), // Silver
                            avatarSize = 54.dp,
                            isDarkMode = isDarkMode,
                            onClick = { onEntryClick(second) }
                        )
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }

                    // 1st Place (Winner - Gold)
                    if (first != null) {
                        PodiumSpot(
                            entry = first,
                            rank = 1,
                            medalColor = Color(0xFFFFB300), // Gold
                            avatarSize = 68.dp,
                            isDarkMode = isDarkMode,
                            onClick = { onEntryClick(first) }
                        )
                    }

                    // 3rd Place (Bronze)
                    if (third != null) {
                        PodiumSpot(
                            entry = third,
                            rank = 3,
                            medalColor = Color(0xFFCD7F32), // Bronze
                            avatarSize = 50.dp,
                            isDarkMode = isDarkMode,
                            onClick = { onEntryClick(third) }
                        )
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }
                }
            }
        }
    }
}

/**
 * Individual Podium Spot for 1st, 2nd, or 3rd place
 */
@Composable
private fun PodiumSpot(
    entry: LeaderboardEntry,
    rank: Int,
    medalColor: Color,
    avatarSize: androidx.compose.ui.unit.Dp,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val name = entry.name

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        // Avatar + Rank badge
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(medalColor.copy(alpha = 0.2f))
                    .border(2.dp, medalColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (entry.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = entry.avatarUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "U",
                        fontWeight = FontWeight.Bold,
                        color = medalColor,
                        fontSize = (avatarSize.value * 0.35).sp
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = medalColor,
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$rank",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Name with optional Verification Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(76.dp)
            )
            if (entry.isVerified) {
                Spacer(modifier = Modifier.width(2.dp))
                VerificationBadge(modifier = Modifier.size(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Profile vs Page Label as requested
        TypeLabelBadge(isPage = entry.isPage, isDarkMode = isDarkMode)

        Spacer(modifier = Modifier.height(2.dp))

        // Score
        Text(
            text = "${NumberFormat.getNumberInstance(Locale.US).format(entry.totalScore.toLong())} pts",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1877F2)
        )
    }
}

/**
 * Individual Card in the Leaderboard List (Identical layout for Profile and Page)
 */
@Composable
private fun LeaderboardCard(
    rank: Int,
    entry: LeaderboardEntry,
    bgCard: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val name = entry.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1877F2).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (entry.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = entry.avatarUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "U",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1877F2),
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User / Page Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadge(modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Profile vs Page label
                TypeLabelBadge(isPage = entry.isPage, isDarkMode = isDarkMode)

                Spacer(modifier = Modifier.height(4.dp))

                // Stats: Likes, Comments, Followers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Likes
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${entry.totalLikes}",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    // Comments
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = Color(0xFF1877F2),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${entry.totalComments}",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    // Followers
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${entry.followersCount}",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Score Points
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${NumberFormat.getNumberInstance(Locale.US).format(entry.totalScore.toLong())} pts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1877F2)
                )
            }
        }
    }
}

/**
 * Clean Badge to indicate whether the item is a Profile or a Page
 * "Profile" for Profile, "Page" for Page
 */
@Composable
private fun TypeLabelBadge(isPage: Boolean, isDarkMode: Boolean) {
    val labelText = if (isPage) "Page" else "Profile"
    val badgeBg = if (isPage) {
        Color(0xFFE8F5E9)
    } else {
        Color(0xFFE3F2FD)
    }
    val badgeTextColor = if (isPage) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFF1565C0)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isDarkMode) badgeTextColor.copy(alpha = 0.2f) else badgeBg,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (isPage) Icons.Default.Flag else Icons.Default.Person,
                contentDescription = null,
                tint = badgeTextColor,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = labelText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeTextColor
            )
        }
    }
}
