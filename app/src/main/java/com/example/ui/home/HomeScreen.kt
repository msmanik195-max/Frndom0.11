package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.mutableStateListOf
import com.example.data.model.FeedSectionType
import com.example.data.model.HomeFeedConfig
import com.example.data.model.HomeFeedSection
import com.example.data.repository.AdminRequestRepository
import com.example.util.formatPostTimestamp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.AdvertisementItem
import com.example.data.repository.AdvertisementRepository
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.components.ExpandableHashtagText
import com.example.ui.components.HashtagText
import com.example.ui.components.PostOptionsBottomSheet
import com.example.ui.components.EditPostDialog
import com.example.data.repository.AppSettingsRepository
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.ui.components.VerificationBadge
import com.example.data.model.PostItem
import com.example.data.model.ReactionType
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.example.data.repository.PostRepository
import com.example.data.repository.StorageRepository
import com.example.data.repository.StoryRepository
import com.example.data.repository.UserRepository
import com.example.data.service.MediaUploadService
import com.example.ui.components.CreateStoryDialog
import com.example.ui.components.FacebookReactionsPopup
import com.example.ui.components.FacebookStoriesTray
import com.example.ui.components.FrndomVideoPlayer
import com.example.ui.components.FullScreenImageViewer
import com.example.ui.components.FullScreenStoryViewer
import com.example.ui.create.PostBackgroundStyle

data class ActiveImageViewerState(
    val urls: List<String>,
    val initialIndex: Int = 0,
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val caption: String = "",
    val isAuthorVerified: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    postRepository: PostRepository,
    storyRepository: StoryRepository,
    onCreatePostClick: () -> Unit,
    storageRepository: StorageRepository? = null,
    mediaUploadService: MediaUploadService? = null,
    onProfileClick: () -> Unit = {},
    onUserClick: (UserProfile) -> Unit = {},
    onCommentClick: (PostItem) -> Unit = {},
    onShareClick: (PostItem) -> Unit = {},
    onMessageClick: ((UserProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val posts by postRepository.postsFlow.collectAsState()
    val isRefreshing by postRepository.isRefreshing.collectAsState()
    val uploadState by postRepository.postUploadState.collectAsState()
    val stories by storyRepository.storiesFlow.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val homeFeedConfig by adminRepo.homeFeedConfigFlow.collectAsState()
    val userRepo = remember { UserRepository.getInstance(context) }
    val allUsers: List<UserProfile> by userRepo.getAllUsersFlow().collectAsState(initial = emptyList())
    val currentUserProfile: UserProfile? by userRepo.currentUserProfileFlow.collectAsState(initial = null)
    val effectiveUser = userProfile ?: currentUserProfile
    val dismissedSuggestions = remember { mutableStateListOf<String>() }

    val activeSections = homeFeedConfig.sections.filter { it.enabled }
    val isVideosSectionEnabled = activeSections.any { it.type == FeedSectionType.VIDEO_POSTS }
    val isImagesSectionEnabled = activeSections.any { it.type == FeedSectionType.IMAGE_POSTS }

    val isFriendsOnly = homeFeedConfig.onlyFriendsPosts
    val effectiveStories = remember(stories, effectiveUser, isFriendsOnly) {
        if (isFriendsOnly && effectiveUser != null) {
            stories.filter { it.userId == effectiveUser.uid || effectiveUser.friendsMap[it.userId] == true }
        } else {
            stories
        }
    }

    val appSettingsRepository = remember { AppSettingsRepository.getInstance(context) }
    val autoPlayVideos by appSettingsRepository.autoPlayVideos.collectAsState()
    val adRepo = remember { AdvertisementRepository.getInstance(context) }
    val allAds by adRepo.advertisementsFlow.collectAsState()
    val adPlacementSettings by adRepo.adPlacementSettingsFlow.collectAsState()

    val feedPosts = remember(posts, effectiveUser, isFriendsOnly, isVideosSectionEnabled, allAds) {
        val approvedAdIds = allAds.filter { it.status == "RUNNING" || it.status == "APPROVED" }.map { it.id }.toSet()
        val unapprovedLinkedPostIds = allAds.filter { it.status != "RUNNING" && it.status != "APPROVED" }.mapNotNull { it.linkedPostId.ifBlank { null } }.toSet()

        val nonReels = posts.filter { post ->
            if (post.mediaType == "reel") return@filter false
            // Filter out unapproved / pending / rejected ad posts
            if (post.advertisementId.isNotBlank() && !approvedAdIds.contains(post.advertisementId)) return@filter false
            if (unapprovedLinkedPostIds.contains(post.id)) return@filter false
            if (post.isSponsored && (post.advertisementId.isBlank() || !approvedAdIds.contains(post.advertisementId))) return@filter false
            true
        }
        val filtered = if (!isVideosSectionEnabled) {
            nonReels.filter { post ->
                val isVideo = post.mediaType.equals("video", ignoreCase = true) ||
                        post.mediaType.equals("reel", ignoreCase = true) ||
                        post.mediaUrl.endsWith(".mp4", ignoreCase = true) ||
                        post.mediaUrl.endsWith(".mkv", ignoreCase = true) ||
                        post.mediaUrl.endsWith(".mov", ignoreCase = true) ||
                        post.mediaUrl.endsWith(".webm", ignoreCase = true) ||
                        post.mediaUrl.contains("/videos/", ignoreCase = true) ||
                        post.mediaUrl.contains("/reels/", ignoreCase = true)
                !isVideo
            }
        } else {
            nonReels
        }
        if (isFriendsOnly && effectiveUser != null) {
            filtered.filter { it.authorId == effectiveUser.uid || effectiveUser.friendsMap[it.authorId] == true }
        } else {
            filtered
        }
    }

    val runningAds = remember(allAds, adPlacementSettings.homeVideoAdsEnabled) {
        allAds.filter { ad ->
            val isRunning = ad.status == "RUNNING" || ad.status == "APPROVED"
            if (!isRunning) return@filter false
            val isVideo = ad.mediaType.equals("video", ignoreCase = true) ||
                    ad.mediaUrl.endsWith(".mp4", ignoreCase = true) ||
                    ad.mediaUrl.contains(".mp4?", ignoreCase = true)
            if (isVideo) {
                adPlacementSettings.homeVideoAdsEnabled
            } else {
                true
            }
        }
    }

    val effectiveMediaUploadService = remember(mediaUploadService, storageRepository) {
        mediaUploadService ?: MediaUploadService(context, storageRepository ?: StorageRepository(context))
    }

    var selectedStoryGroupsToView by remember { mutableStateOf<Pair<Int, List<com.example.ui.components.UserStoryGroup>>?>(null) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    
    // Comments Bottom Sheet State
    var showCommentsSheet by remember { mutableStateOf<PostItem?>(null) }

    // Post Options Bottom Sheet & Edit Dialog State
    var selectedPostForOptions by remember { mutableStateOf<PostItem?>(null) }
    var editingPost by remember { mutableStateOf<PostItem?>(null) }

    // Full Screen Image Viewer State
    var activeImageViewerData by remember { mutableStateOf<ActiveImageViewerState?>(null) }

    val initial = userProfile?.firstName?.firstOrNull()?.uppercase()
        ?: userProfile?.fullName?.firstOrNull()?.uppercase()
        ?: "U"
    val userId = userProfile?.uid ?: "user_id"

    val allImagePosts = remember(feedPosts) {
        feedPosts.filter { it.mediaType == "photo" || it.mediaUrls.isNotEmpty() || (it.mediaUrl.isNotBlank() && it.mediaType != "video" && it.mediaType != "reel") }
    }
    val allVideoPosts = remember(feedPosts, isVideosSectionEnabled) {
        if (!isVideosSectionEnabled) emptyList()
        else feedPosts.filter { it.mediaType == "video" || it.mediaUrl.endsWith(".mp4", ignoreCase = true) }
    }
    val allTextPosts = remember(feedPosts) {
        feedPosts.filter { it.mediaType == "text" || (it.mediaUrl.isBlank() && it.mediaUrls.isEmpty()) }
    }
    val friendSuggestions = remember(allUsers, effectiveUser, userId, dismissedSuggestions) {
        val myUid = effectiveUser?.uid?.ifBlank { userId } ?: userId
        val myEmail = effectiveUser?.email?.trim()?.lowercase() ?: ""
        allUsers.filter { user: UserProfile ->
            val uUid = user.uid.trim()
            val uEmail = user.email.trim().lowercase()
            uUid.isNotBlank() &&
            uUid != myUid &&
            uUid != userId &&
            (myEmail.isBlank() || uEmail.isBlank() || uEmail != myEmail) &&
            effectiveUser?.friendsMap?.get(uUid) != true &&
            !dismissedSuggestions.contains(uUid)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                postRepository.refreshPosts()
                storyRepository.refreshStories()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("home_screen_feed")
            ) {
                // 1. "What's on your mind?" Top Bar
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compose_post_bar"),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Avatar
                            Surface(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onProfileClick)
                                    .testTag("home_user_avatar"),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                if (!userProfile?.profilePictureUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = userProfile?.profilePictureUrl,
                                        contentDescription = "User Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = initial,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Input Box Pill
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(onClick = onCreatePostClick)
                                    .testTag("whats_on_your_mind_button"),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = "What's on your mind?",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Green Gallery/Photo Icon Button
                            IconButton(
                                onClick = onCreatePostClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("home_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Photos",
                                    tint = Color(0xFF45BD62),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2. Background Upload Progress Card (under composer)
                if (uploadState.isUploading) {
                    item(key = "post_upload_progress_card") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .testTag("upload_progress_card"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Media preview or icon
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val previewUri = uploadState.previewUri
                                    if (previewUri != null) {
                                        AsyncImage(
                                            model = previewUri,
                                            contentDescription = "Upload Preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Uploading",
                                            tint = Color(0xFF1877F2),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = uploadState.statusText.ifBlank { "Uploading post..." },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${uploadState.progressPercent}%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1877F2)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { uploadState.progress.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF1877F2),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // 3. Dynamic Feed Sections based on Admin Feed Customization
                val isLoadingInitial = feedPosts.isEmpty() && posts.isEmpty() && !isRefreshing
                if (isRefreshing || isLoadingInitial) {
                    items(3) {
                        ShimmerPostCard()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else if (feedPosts.isEmpty() && effectiveStories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isFriendsOnly) "No friends' posts yet" else "No posts yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isFriendsOnly) "Add friends to see their stories and posts here!" else "Tap the '+' button or 'What's on your mind?' to share a post!",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }
                } else {
                    val consumedPostIds = mutableSetOf<String>()

                    activeSections.forEach { section ->
                        when (section.type) {
                            FeedSectionType.STORIES -> {
                                item(key = "stories_${section.id}") {
                                    FacebookStoriesTray(
                                        userProfile = userProfile,
                                        stories = effectiveStories,
                                        onCreateStoryClick = { showCreateStoryDialog = true },
                                        onStoryGroupClick = { clickedGroup, allGroups ->
                                            val idx = allGroups.indexOfFirst { it.userId == clickedGroup.userId }.coerceAtLeast(0)
                                            selectedStoryGroupsToView = Pair(idx, allGroups)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            FeedSectionType.IMAGE_POSTS -> {
                                val availableImages = allImagePosts.filter { it.id !in consumedPostIds }
                                val count = section.maxCount.coerceAtLeast(1)
                                val batch = availableImages.take(count)
                                batch.forEach { post ->
                                    consumedPostIds.add(post.id)
                                    item(key = "img_${section.id}_${post.id}") {
                                        PostCardItem(
                                            post = post,
                                            currentUserId = userId,
                                            currentUserProfile = userProfile,
                                            postRepository = postRepository,
                                            autoPlayVideos = autoPlayVideos,
                                            onUserClick = {
                                                val peer = UserProfile(
                                                    uid = post.authorId,
                                                    fullName = post.authorName,
                                                    profilePictureUrl = post.authorAvatarUrl
                                                )
                                                onUserClick(peer)
                                            },
                                            onOptionsClick = { selectedPostForOptions = post },
                                            onLikeClick = { postRepository.toggleLike(post.id, userId) },
                                            onReactionClick = { r -> postRepository.setReaction(post.id, userId, r) },
                                            onCommentClick = { showCommentsSheet = post },
                                            onShareClick = {
                                                postRepository.incrementShare(post.id)
                                                onShareClick(post)
                                            },
                                            onImageClick = { urls, index ->
                                                val isAuthorVerified = post.isAuthorVerified || (userProfile != null && post.authorId == userProfile.uid && userProfile.isVerificationActive()) || UserRepository.isUserVerifiedStatic(post.authorId)
                                                activeImageViewerData = ActiveImageViewerState(
                                                    urls = urls,
                                                    initialIndex = index,
                                                    authorName = post.authorName,
                                                    authorAvatarUrl = post.authorAvatarUrl,
                                                    caption = post.content,
                                                    isAuthorVerified = isAuthorVerified
                                                )
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            FeedSectionType.FRIEND_SUGGESTIONS -> {
                                val count = section.maxCount.coerceAtLeast(1)
                                val suggestionsBatch = friendSuggestions.take(count)
                                if (suggestionsBatch.isNotEmpty()) {
                                    item(key = "suggestions_${section.id}") {
                                        FriendSuggestionsFeedTray(
                                            suggestions = suggestionsBatch,
                                            currentUserId = userId,
                                            currentUserProfile = effectiveUser,
                                            userRepository = userRepo,
                                            onUserClick = onUserClick,
                                            onDismissSuggestion = { uid -> dismissedSuggestions.add(uid) }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            FeedSectionType.VIDEO_POSTS -> {
                                val availableVideos = allVideoPosts.filter { it.id !in consumedPostIds }
                                val count = section.maxCount.coerceAtLeast(1)
                                val batch = availableVideos.take(count)
                                batch.forEach { post ->
                                    consumedPostIds.add(post.id)
                                    item(key = "vid_${section.id}_${post.id}") {
                                        PostCardItem(
                                            post = post,
                                            currentUserId = userId,
                                            currentUserProfile = userProfile,
                                            postRepository = postRepository,
                                            autoPlayVideos = autoPlayVideos,
                                            onUserClick = {
                                                val peer = UserProfile(
                                                    uid = post.authorId,
                                                    fullName = post.authorName,
                                                    profilePictureUrl = post.authorAvatarUrl
                                                )
                                                onUserClick(peer)
                                            },
                                            onOptionsClick = { selectedPostForOptions = post },
                                            onLikeClick = { postRepository.toggleLike(post.id, userId) },
                                            onReactionClick = { r -> postRepository.setReaction(post.id, userId, r) },
                                            onCommentClick = { showCommentsSheet = post },
                                            onShareClick = {
                                                postRepository.incrementShare(post.id)
                                                onShareClick(post)
                                            },
                                            onImageClick = { urls, index ->
                                                val isAuthorVerified = post.isAuthorVerified || (userProfile != null && post.authorId == userProfile.uid && userProfile.isVerificationActive()) || UserRepository.isUserVerifiedStatic(post.authorId)
                                                activeImageViewerData = ActiveImageViewerState(
                                                    urls = urls,
                                                    initialIndex = index,
                                                    authorName = post.authorName,
                                                    authorAvatarUrl = post.authorAvatarUrl,
                                                    caption = post.content,
                                                    isAuthorVerified = isAuthorVerified
                                                )
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            FeedSectionType.TEXT_POSTS -> {
                                val availableTexts = allTextPosts.filter { it.id !in consumedPostIds }
                                val count = section.maxCount.coerceAtLeast(1)
                                val batch = availableTexts.take(count)
                                batch.forEach { post ->
                                    consumedPostIds.add(post.id)
                                    item(key = "txt_${section.id}_${post.id}") {
                                        PostCardItem(
                                            post = post,
                                            currentUserId = userId,
                                            currentUserProfile = userProfile,
                                            postRepository = postRepository,
                                            autoPlayVideos = autoPlayVideos,
                                            onUserClick = {
                                                val peer = UserProfile(
                                                    uid = post.authorId,
                                                    fullName = post.authorName,
                                                    profilePictureUrl = post.authorAvatarUrl
                                                )
                                                onUserClick(peer)
                                            },
                                            onOptionsClick = { selectedPostForOptions = post },
                                            onLikeClick = { postRepository.toggleLike(post.id, userId) },
                                            onReactionClick = { r -> postRepository.setReaction(post.id, userId, r) },
                                            onCommentClick = { showCommentsSheet = post },
                                            onShareClick = {
                                                postRepository.incrementShare(post.id)
                                                onShareClick(post)
                                            },
                                            onImageClick = { _, _ -> }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    // Render remaining posts that haven't been shown in the configured blocks
                    val remainingPosts = feedPosts.filter { it.id !in consumedPostIds }
                    itemsIndexed(remainingPosts, key = { _, post -> "rem_${post.id}" }) { index, post ->
                        PostCardItem(
                            post = post,
                            currentUserId = userId,
                            currentUserProfile = userProfile,
                            postRepository = postRepository,
                            autoPlayVideos = autoPlayVideos,
                            onUserClick = {
                                val peer = UserProfile(
                                    uid = post.authorId,
                                    fullName = post.authorName,
                                    profilePictureUrl = post.authorAvatarUrl
                                )
                                onUserClick(peer)
                            },
                            onOptionsClick = {
                                selectedPostForOptions = post
                            },
                            onLikeClick = { postRepository.toggleLike(post.id, userId) },
                            onReactionClick = { r -> postRepository.setReaction(post.id, userId, r) },
                            onCommentClick = {
                                showCommentsSheet = post
                            },
                            onShareClick = {
                                postRepository.incrementShare(post.id)
                                onShareClick(post)
                            },
                            onImageClick = { urls, index ->
                                val isAuthorVerified = post.isAuthorVerified || (userProfile != null && post.authorId == userProfile.uid && userProfile.isVerificationActive()) || UserRepository.isUserVerifiedStatic(post.authorId)
                                activeImageViewerData = ActiveImageViewerState(
                                    urls = urls,
                                    initialIndex = index,
                                    authorName = post.authorName,
                                    authorAvatarUrl = post.authorAvatarUrl,
                                    caption = post.content,
                                    isAuthorVerified = isAuthorVerified
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Sponsored Ad insertion based on Admin Advertisement Placement setting
                        val homeInterval = adPlacementSettings.homePostInterval.coerceAtLeast(1)
                        if (runningAds.isNotEmpty() && ((index + 1) % homeInterval == 0)) {
                            val adIndex = ((index + 1) / homeInterval - 1) % runningAds.size
                            val adToShow = runningAds[adIndex]
                            SponsoredAdFeedCard(
                                ad = adToShow,
                                onAdClick = { clickedAd ->
                                    adRepo.recordClick(clickedAd.id)
                                },
                                onMessageClick = onMessageClick
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Full Screen Story Viewer
        selectedStoryGroupsToView?.let { (initialIndex, groups) ->
            FullScreenStoryViewer(
                storyGroups = groups,
                initialGroupIndex = initialIndex,
                currentUserProfile = userProfile,
                storyRepository = storyRepository,
                onDismiss = { selectedStoryGroupsToView = null }
            )
        }

        // Full Screen Multi-Image Viewer
        activeImageViewerData?.let { data ->
            FullScreenImageViewer(
                images = data.urls,
                initialIndex = data.initialIndex,
                title = "Photo",
                authorName = data.authorName,
                authorAvatarUrl = data.authorAvatarUrl,
                caption = data.caption,
                isAuthorVerified = data.isAuthorVerified,
                onDismiss = { activeImageViewerData = null }
            )
        }

        // Create Story Dialog
        if (showCreateStoryDialog) {
            CreateStoryDialog(
                userProfile = userProfile,
                mediaUploadService = effectiveMediaUploadService,
                onStoryCreated = { newStory ->
                    storyRepository.createStory(newStory)
                    showCreateStoryDialog = false
                },
                onDismiss = { showCreateStoryDialog = false }
            )
        }

        // Comments Bottom Sheet
        showCommentsSheet?.let { post ->
            com.example.ui.components.CommentsBottomSheet(
                postRepository = postRepository,
                postId = post.id,
                userProfile = userProfile,
                onDismiss = { showCommentsSheet = null },
                onCommentAdded = { }
            )
        }

        // Post Options Bottom Sheet (Edit, Delete, Save, Copy Link, Report)
        selectedPostForOptions?.let { post ->
            PostOptionsBottomSheet(
                post = post,
                currentUserId = userId,
                postRepository = postRepository,
                onEditClick = { editingPost = it },
                onDeletePost = {
                    selectedPostForOptions = null
                },
                onDismiss = { selectedPostForOptions = null }
            )
        }

        // Edit Post Dialog (Edit text, replace photos, locked video for video posts)
        editingPost?.let { post ->
            EditPostDialog(
                post = post,
                postRepository = postRepository,
                mediaUploadService = effectiveMediaUploadService,
                onDismiss = { editingPost = null },
                onPostUpdated = {
                    editingPost = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCardItem(
    post: PostItem,
    currentUserId: String,
    currentUserProfile: UserProfile? = null,
    postRepository: PostRepository? = null,
    autoPlayVideos: Boolean? = null,
    onUserClick: () -> Unit = {},
    onOptionsClick: () -> Unit = {},
    onLikeClick: () -> Unit,
    onReactionClick: (ReactionType) -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onImageClick: ((urls: List<String>, index: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val appSettingsRepo = remember { AppSettingsRepository.getInstance(context) }
    val globalAutoPlay by appSettingsRepo.autoPlayVideos.collectAsState()
    val isAutoPlayActive = autoPlayVideos ?: globalAutoPlay

    if (postRepository != null && currentUserId.isNotBlank()) {
        androidx.compose.runtime.LaunchedEffect(post.id, currentUserId) {
            postRepository.recordPostView(post.id, currentUserId)
            com.example.data.repository.WatchHistoryRepository.getInstance(context).recordHistory(post, currentUserId)
        }
    } else {
        androidx.compose.runtime.LaunchedEffect(post.id) {
            com.example.data.repository.WatchHistoryRepository.getInstance(context).recordHistory(post, currentUserId)
        }
    }

    val userReaction = post.getUserReaction(currentUserId)
    val isVerifiedAuthor = post.isAuthorVerified || (currentUserProfile != null && post.authorId == currentUserProfile.uid && currentUserProfile.isVerificationActive()) || UserRepository.isUserVerifiedStatic(post.authorId)
    val liveAvatar = UserRepository.getUserAvatarStatic(post.authorId)

    val adRepo = remember { AdvertisementRepository.getInstance(context) }
    val allAds by adRepo.advertisementsFlow.collectAsState()
    val isSponsoredActive = remember(allAds, post.id, post.advertisementId, post.isSponsored) {
        val linkedAd = allAds.firstOrNull { it.linkedPostId == post.id || (post.advertisementId.isNotBlank() && it.id == post.advertisementId) }
        if (linkedAd != null) {
            linkedAd.status == "RUNNING" || linkedAd.status == "APPROVED"
        } else {
            post.isSponsored
        }
    }
    val effectiveAuthorAvatar = if (currentUserProfile != null && post.authorId == currentUserProfile.uid && currentUserProfile.profilePictureUrl.isNotBlank()) {
        currentUserProfile.profilePictureUrl
    } else if (liveAvatar.isNotBlank()) {
        liveAvatar
    } else {
        post.authorAvatarUrl
    }
    val authorInitial = post.authorName.firstOrNull()?.uppercase() ?: "U"

    var showReactionsTray by remember { mutableStateOf(false) }

    val bgStyle = PostBackgroundStyle.entries.firstOrNull { it.id == post.backgroundStyle } ?: PostBackgroundStyle.NONE
    val hasBackground = bgStyle != PostBackgroundStyle.NONE && bgStyle != PostBackgroundStyle.WHITE

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onUserClick)
                        .padding(2.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (effectiveAuthorAvatar.isNotBlank()) {
                            AsyncImage(
                                model = effectiveAuthorAvatar,
                                contentDescription = post.authorName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = authorInitial,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        if (post.groupName.isNotBlank()) {
                            Text(
                                text = post.groupName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = post.authorName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1877F2)
                                )
                                if (isVerifiedAuthor) {
                                    VerificationBadge(size = 14.dp, show = true)
                                }
                                if (isSponsoredActive) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF1877F2).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Sponsored",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1877F2),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = " • ${formatPostTimestamp(post.createdAt)} • ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = when (post.audience) {
                                        "Private" -> Icons.Default.Lock
                                        else -> Icons.Default.Public
                                    },
                                    contentDescription = post.audience,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = post.authorName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isVerifiedAuthor) {
                                    VerificationBadge(size = 16.dp, show = true)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSponsoredActive) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF1877F2).copy(alpha = 0.12f),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            text = "Sponsored",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1877F2),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = "• ",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatPostTimestamp(post.createdAt),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = " • ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = when (post.audience) {
                                        "Friends" -> Icons.Default.Group
                                        "Only Me" -> Icons.Default.Lock
                                        else -> Icons.Default.Public
                                    },
                                    contentDescription = post.audience,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onOptionsClick) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Post Content
            if (hasBackground) {
                // Colored / Gradient Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .then(
                            if (bgStyle.isGradient) {
                                Modifier.background(Brush.linearGradient(bgStyle.gradientColors))
                            } else {
                                Modifier.background(bgStyle.singleColor)
                            }
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HashtagText(
                        text = post.content,
                        fontSize = post.fontSize.sp,
                        fontWeight = FontWeight.Bold,
                        color = bgStyle.textColor,
                        hashtagColor = if (bgStyle.textColor == Color.White) Color(0xFF80D8FF) else Color(0xFF1877F2),
                        textAlign = when (post.textAlign) {
                            "left" -> TextAlign.Left
                            "right" -> TextAlign.Right
                            else -> TextAlign.Center
                        },
                        lineHeight = (post.fontSize + 6).sp
                    )
                }
            } else {
                // Normal Text Post
                if (post.content.isNotEmpty()) {
                    ExpandableHashtagText(
                        text = post.content,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        hashtagColor = Color(0xFF1877F2),
                        lineHeight = 21.sp,
                        collapsedMaxLines = 3,
                        textAlign = when (post.textAlign) {
                            "left" -> TextAlign.Left
                            "right" -> TextAlign.Right
                            else -> TextAlign.Start
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Media Video or Images
                if (post.mediaType == "video" || post.mediaType == "reel") {
                    if (post.mediaUrl.isNotBlank()) {
                        FrndomVideoPlayer(
                            videoUrl = post.mediaUrl,
                            autoPlay = isAutoPlayActive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    }
                } else {
                    val allUrls = post.getAllMediaUrls()
                    if (allUrls.isNotEmpty()) {
                        PostMediaGrid(
                            urls = allUrls,
                            onImageClick = { urls, index ->
                                onImageClick?.invoke(urls, index)
                            }
                        )
                    }
                }
            }

            // Reactions Counts Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (post.likesCount > 0) {
                        val topReactions = post.getTopReactions()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            topReactions.forEachIndexed { index, reaction ->
                                Box(
                                    modifier = Modifier
                                        .offset(x = if (index > 0) (-4 * index).dp else 0.dp)
                                        .zIndex((topReactions.size - index).toFloat())
                                ) {
                                    Text(
                                        text = reaction.emoji,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${post.likesCount}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (post.viewsCount > 0) {
                        Text(
                            text = "${post.viewsCount} ${if (post.viewsCount == 1) "view" else "views"}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (post.commentsCount > 0) {
                        Text(
                            text = "${post.commentsCount} comments",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (post.sharesCount > 0) {
                        Text(
                            text = "${post.sharesCount} shares",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Action Buttons: Like, Comment, Share with Floating Reaction popup
            Box(modifier = Modifier.fillMaxWidth()) {
                // Floating Reactions Tray
                if (showReactionsTray) {
                    Box(
                        modifier = Modifier
                            .offset(x = 12.dp, y = (-36).dp)
                            .zIndex(10f)
                    ) {
                        FacebookReactionsPopup(
                            onReactionSelected = { reaction ->
                                onReactionClick(reaction)
                                showReactionsTray = false
                            },
                            onDismiss = { showReactionsTray = false }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Like Button (Click = Like toggle, Long Click = Reactions popup)
                    Row(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = onLikeClick,
                                onLongClick = { showReactionsTray = true }
                            )
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .testTag("post_like_button_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (userReaction != null) {
                            Text(
                                text = userReaction.emoji,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = userReaction.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(userReaction.colorHex)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Like",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Comment Button
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onCommentClick)
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .testTag("post_comment_button_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Comment",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share Button
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onShareClick)
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .testTag("post_share_button_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostMediaGrid(
    urls: List<String>,
    modifier: Modifier = Modifier,
    onImageClick: (urls: List<String>, index: Int) -> Unit = { _, _ -> }
) {
    if (urls.isEmpty()) return

    when (urls.size) {
        1 -> {
            // Adaptive Single Image: Fits wide (16:9), square (1:1), and vertical (4:5 / 9:16) without cutting off!
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(Color(0xFF0F0F0F))
                    .clip(RoundedCornerShape(0.dp))
                    .clickable { onImageClick(urls, 0) },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = urls[0],
                    contentDescription = "Post Image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(min = 180.dp, max = 560.dp)
                )
            }
        }
        2 -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onImageClick(urls, 0) }
                ) {
                    AsyncImage(
                        model = urls[0],
                        contentDescription = "Photo 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onImageClick(urls, 1) }
                ) {
                    AsyncImage(
                        model = urls[1],
                        contentDescription = "Photo 2",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        3 -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxSize()
                        .clickable { onImageClick(urls, 0) }
                ) {
                    AsyncImage(
                        model = urls[0],
                        contentDescription = "Photo 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable { onImageClick(urls, 1) }
                    ) {
                        AsyncImage(
                            model = urls[1],
                            contentDescription = "Photo 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable { onImageClick(urls, 2) }
                    ) {
                        AsyncImage(
                            model = urls[2],
                            contentDescription = "Photo 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        else -> {
            // 4 or more photos (up to 10) with 2x2 grid & +N overlay
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Top row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onImageClick(urls, 0) }
                    ) {
                        AsyncImage(
                            model = urls[0],
                            contentDescription = "Photo 1",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onImageClick(urls, 1) }
                    ) {
                        AsyncImage(
                            model = urls[1],
                            contentDescription = "Photo 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Bottom row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onImageClick(urls, 2) }
                    ) {
                        AsyncImage(
                            model = urls[2],
                            contentDescription = "Photo 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onImageClick(urls, 3) }
                    ) {
                        AsyncImage(
                            model = urls[3],
                            contentDescription = "Photo 4",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (urls.size > 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${urls.size - 3}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerPostCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    val shimmerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(shimmerColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Box(modifier = Modifier.height(14.dp).width(120.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.height(10.dp).width(80.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.9f).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.7f).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Media Image Placeholder
            Box(modifier = Modifier.height(200.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(shimmerColor))
        }
    }
}

@Composable
fun SponsoredAdFeedCard(
    ad: AdvertisementItem,
    onAdClick: (AdvertisementItem) -> Unit,
    onMessageClick: ((UserProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(ad.id) {
        AdvertisementRepository.getInstance(context).recordImpression(ad.id)
    }

    val handleActionClick = {
        onAdClick(ad)
        if (ad.callToAction.equals("Send Message", ignoreCase = true) || ad.destinationUrl.startsWith("chat:")) {
            val peer = UserProfile(
                uid = ad.userId,
                firstName = ad.userName.substringBefore(" "),
                lastName = ad.userName.substringAfter(" ", ""),
                fullName = ad.userName,
                profilePictureUrl = ad.userAvatar
            )
            onMessageClick?.invoke(peer)
        } else if (ad.destinationUrl.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.destinationUrl))
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(42.dp)
                ) {
                    if (ad.userAvatar.isNotBlank()) {
                        AsyncImage(
                            model = ad.userAvatar,
                            contentDescription = ad.userName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = ad.userName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1877F2)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ad.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sponsored",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = " • ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Public",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Ad Headline & Description
            if (ad.headline.isNotBlank()) {
                Text(
                    text = ad.headline,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                )
            }
            if (ad.description.isNotBlank()) {
                Text(
                    text = ad.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ad Media Banner (Photo or Video)
            if (ad.mediaUrl.isNotBlank()) {
                val isVideo = ad.mediaType == "video" || ad.mediaUrl.endsWith(".mp4", ignoreCase = true) || ad.mediaUrl.contains(".mp4?", ignoreCase = true)
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 340.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        FrndomVideoPlayer(
                            videoUrl = ad.mediaUrl,
                            modifier = Modifier.fillMaxSize(),
                            autoPlay = true,
                            isLooping = true
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = ad.mediaUrl,
                            contentDescription = "Sponsored Ad",
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(min = 180.dp, max = 560.dp)
                                .clickable {
                                    handleActionClick()
                                },
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }

            // Bottom CTA Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable {
                        handleActionClick()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    val domain = if (ad.callToAction.equals("Send Message", ignoreCase = true) || ad.destinationUrl.startsWith("chat:")) {
                        "MESSENGER CHAT"
                    } else {
                        try {
                            val host = Uri.parse(ad.destinationUrl).host.orEmpty()
                            if (host.isNotBlank()) host.uppercase() else "PROMOTION"
                        } catch (_: Exception) {
                            "PROMOTION"
                        }
                    }
                    Text(
                        text = domain,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = ad.campaignName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        handleActionClick()
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = ad.callToAction.ifBlank { "Learn More" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun FriendSuggestionsFeedTray(
    suggestions: List<UserProfile>,
    currentUserId: String,
    currentUserProfile: UserProfile?,
    userRepository: UserRepository,
    onUserClick: (UserProfile) -> Unit,
    onDismissSuggestion: (String) -> Unit
) {
    if (suggestions.isEmpty()) return
    val context = androidx.compose.ui.platform.LocalContext.current
    val appSettingsRepo = remember { AppSettingsRepository.getInstance(context) }
    val isDarkMode by appSettingsRepo.isDarkMode.collectAsState()
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val cardBorder = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = Color(0xFF1877F2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "People You May Know",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(suggestions, key = { it.uid }) { user ->
                    val isRequested = currentUserProfile?.friendRequestsSentMap?.get(user.uid) == true
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .clickable { onUserClick(user) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(140.dp, 130.dp)) {
                                if (user.profilePictureUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.profilePictureUrl,
                                        contentDescription = user.fullName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF1877F2).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.fullName.firstOrNull()?.uppercase() ?: "U",
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1877F2)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDismissSuggestion(user.uid) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(26.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = user.fullName.ifBlank { "User" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (isRequested) {
                                            userRepository.cancelFriendRequest(currentUserId, user.uid)
                                        } else {
                                            userRepository.sendFriendRequest(currentUserId, user.uid)
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRequested) (if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)) else Color(0xFF1877F2),
                                        contentColor = if (isRequested) textPrimary else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                ) {
                                    Text(
                                        text = if (isRequested) "Requested" else "Add Friend",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
