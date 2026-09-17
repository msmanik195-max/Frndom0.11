package com.example.ui.menu.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.model.PostItem
import com.example.data.model.StoryItem
import com.example.data.repository.PostRepository
import com.example.data.repository.StoryRepository
import java.text.SimpleDateFormat
import java.util.*

enum class FileManagerTab {
    IMAGES,
    VIDEOS,
    STORIES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFileManagerView(
    postRepository: PostRepository,
    storyRepository: StoryRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()

    val allPosts by postRepository.postsFlow.collectAsState()
    val allStories by storyRepository.storiesFlow.collectAsState()

    var selectedTab by remember { mutableStateOf(FileManagerTab.IMAGES) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var postToDelete by remember { mutableStateOf<PostItem?>(null) }
    var storyToDelete by remember { mutableStateOf<StoryItem?>(null) }

    // Image posts: mediaType == photo or has mediaUrl/mediaUrls and not video/reel
    val imagePosts = remember(allPosts) {
        allPosts.filter { post ->
            val isVideo = post.mediaType.equals("video", ignoreCase = true) || post.mediaType.equals("reel", ignoreCase = true)
            !isVideo && (post.mediaType.equals("photo", ignoreCase = true) || post.mediaUrls.isNotEmpty() || post.mediaUrl.isNotBlank())
        }
    }

    // Video posts: mediaType == video or reel or mediaUrl ends with video extension
    val videoPosts = remember(allPosts) {
        allPosts.filter { post ->
            post.mediaType.equals("video", ignoreCase = true) || post.mediaType.equals("reel", ignoreCase = true)
        }
    }

    // Filtered lists based on search
    val filteredImages = remember(imagePosts, searchQuery) {
        if (searchQuery.isBlank()) imagePosts
        else imagePosts.filter {
            it.content.contains(searchQuery, ignoreCase = true) ||
                    it.authorName.contains(searchQuery, ignoreCase = true) ||
                    it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredVideos = remember(videoPosts, searchQuery) {
        if (searchQuery.isBlank()) videoPosts
        else videoPosts.filter {
            it.content.contains(searchQuery, ignoreCase = true) ||
                    it.authorName.contains(searchQuery, ignoreCase = true) ||
                    it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredStories = remember(allStories, searchQuery) {
        if (searchQuery.isBlank()) allStories
        else allStories.filter {
            it.caption.contains(searchQuery, ignoreCase = true) ||
                    it.userName.contains(searchQuery, ignoreCase = true) ||
                    it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    val bgColor = if (isDarkMode) Color(0xFF101216) else Color(0xFFF0F2F5)
    val cardBg = if (isDarkMode) Color(0xFF1E222B) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF1C1E21)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF2E333D) else Color(0xFFE4E6EB)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "File Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Manage uploaded images, videos & stories",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_file_manager_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
            )
        },
        containerColor = bgColor,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs & Search Header
            Surface(
                color = cardBg,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Tabs
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = cardBg,
                        contentColor = Color(0xFF1877F2),
                        divider = { HorizontalDivider(color = dividerColor) }
                    ) {
                        Tab(
                            selected = selectedTab == FileManagerTab.IMAGES,
                            onClick = { selectedTab = FileManagerTab.IMAGES },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Images (${imagePosts.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == FileManagerTab.VIDEOS,
                            onClick = { selectedTab = FileManagerTab.VIDEOS },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Videos (${videoPosts.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == FileManagerTab.STORIES,
                            onClick = { selectedTab = FileManagerTab.STORIES },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stories (${allStories.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        )
                    }

                    // Search input
                    Box(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_file_manager_search_input"),
                            placeholder = {
                                Text(
                                    when (selectedTab) {
                                        FileManagerTab.IMAGES -> "Search image posts by author or caption..."
                                        FileManagerTab.VIDEOS -> "Search videos / reels by author or caption..."
                                        FileManagerTab.STORIES -> "Search stories by author or caption..."
                                    },
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = textSecondary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = textSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = dividerColor
                            )
                        )
                    }
                }
            }

            // Tab Content Grid
            when (selectedTab) {
                FileManagerTab.IMAGES -> {
                    if (filteredImages.isEmpty()) {
                        EmptyFileManagerState(
                            icon = Icons.Default.PhotoLibrary,
                            title = if (searchQuery.isNotBlank()) "No matching image posts" else "No image posts found",
                            subtitle = "Uploaded image posts will appear here for management.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredImages, key = { it.id }) { post ->
                                ImageFileManagerCard(
                                    post = post,
                                    cardBg = cardBg,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    dividerColor = dividerColor,
                                    onDeleteClick = { postToDelete = post }
                                )
                            }
                        }
                    }
                }

                FileManagerTab.VIDEOS -> {
                    if (filteredVideos.isEmpty()) {
                        EmptyFileManagerState(
                            icon = Icons.Default.VideoLibrary,
                            title = if (searchQuery.isNotBlank()) "No matching videos or reels" else "No video posts found",
                            subtitle = "Uploaded videos and reels will appear here for management.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredVideos, key = { it.id }) { post ->
                                VideoFileManagerCard(
                                    post = post,
                                    cardBg = cardBg,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    dividerColor = dividerColor,
                                    onDeleteClick = { postToDelete = post }
                                )
                            }
                        }
                    }
                }

                FileManagerTab.STORIES -> {
                    if (filteredStories.isEmpty()) {
                        EmptyFileManagerState(
                            icon = Icons.Default.HistoryEdu,
                            title = if (searchQuery.isNotBlank()) "No matching stories" else "No active stories found",
                            subtitle = "User created stories will appear here for management.",
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredStories, key = { it.id }) { story ->
                                StoryFileManagerCard(
                                    story = story,
                                    cardBg = cardBg,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    dividerColor = dividerColor,
                                    onDeleteClick = { storyToDelete = story }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog: Delete Post (Image or Video)
    postToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Delete Post from Database?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to completely remove this post from the database?",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Author: ${post.authorName.ifBlank { "User" }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    if (post.content.isNotBlank()) {
                        Text(
                            "Caption: ${post.content.take(80)}...",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "This will immediately and permanently delete this content from Firebase Realtime Database and the entire app.",
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        postRepository.deletePost(post.id)
                        postToDelete = null
                        Toast.makeText(context, "Post permanently deleted from database", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete from Database", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { postToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirmation Dialog: Delete Story
    storyToDelete?.let { story ->
        AlertDialog(
            onDismissRequest = { storyToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Delete Story from Database?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to completely remove this story from the database?",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "User: ${story.userName.ifBlank { "User" }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "This will immediately and permanently delete this story from Firebase Realtime Database and the app.",
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        storyRepository.deleteStory(story.id)
                        storyToDelete = null
                        Toast.makeText(context, "Story permanently deleted from database", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Story", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { storyToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ImageFileManagerCard(
    post: PostItem,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
    onDeleteClick: () -> Unit
) {
    val mediaUrls = post.getAllMediaUrls()
    val displayUrl = mediaUrls.firstOrNull() ?: post.mediaUrl

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Image Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black)
            ) {
                if (displayUrl.isNotBlank()) {
                    AsyncImage(
                        model = displayUrl,
                        contentDescription = "Post Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2E333D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray)
                    }
                }

                // Multi-photo count badge
                if (mediaUrls.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "${mediaUrls.size} photos",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Details
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1877F2),
                        modifier = Modifier.size(20.dp)
                    ) {
                        if (post.authorAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = post.authorAvatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (post.authorName.firstOrNull() ?: 'U').uppercase(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.authorName.ifBlank { "User" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (post.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.content,
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Delete Button
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete from DB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun VideoFileManagerCard(
    post: PostItem,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
    onDeleteClick: () -> Unit
) {
    val displayUrl = post.mediaUrl.ifBlank { post.mediaUrls.firstOrNull() ?: "" }
    val isReel = post.mediaType.equals("reel", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Video Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black)
            ) {
                if (displayUrl.isNotBlank()) {
                    AsyncImage(
                        model = displayUrl,
                        contentDescription = "Video Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Play Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // Type Badge (Reel vs Video)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isReel) Color(0xFFE1306C) else Color(0xFF1877F2),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (isReel) "REEL" else "VIDEO",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Details
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1877F2),
                        modifier = Modifier.size(20.dp)
                    ) {
                        if (post.authorAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = post.authorAvatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (post.authorName.firstOrNull() ?: 'U').uppercase(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.authorName.ifBlank { "User" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (post.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.content,
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Delete Button
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete from DB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StoryFileManagerCard(
    story: StoryItem,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Story preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFF282C34))
            ) {
                if (story.mediaUrl.isNotBlank()) {
                    AsyncImage(
                        model = story.mediaUrl,
                        contentDescription = "Story Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Text story preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1877F2).copy(alpha = 0.8f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = story.caption.ifBlank { "Text Story" },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Viewers badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${story.viewersCount}",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Details
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1877F2),
                        modifier = Modifier.size(20.dp)
                    ) {
                        if (story.userAvatar.isNotBlank()) {
                            AsyncImage(
                                model = story.userAvatar,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (story.userName.firstOrNull() ?: 'U').uppercase(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = story.userName.ifBlank { "User" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (story.caption.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = story.caption,
                        fontSize = 11.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Delete Button
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete from DB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyFileManagerState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1877F2).copy(alpha = 0.12f),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(36.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = textPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = subtitle, fontSize = 13.sp, color = textSecondary, textAlign = TextAlign.Center)
        }
    }
}
