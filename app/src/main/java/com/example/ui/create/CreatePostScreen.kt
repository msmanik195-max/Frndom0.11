package com.example.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.components.DEFAULT_POPULAR_HASHTAGS
import com.example.ui.components.HashtagVisualTransformation
import com.example.util.MediaUriHelper
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.ContentLimitManager
import com.example.data.repository.ContentValidationResult
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.data.service.MediaUploadService
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.LocalIsDarkMode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

enum class PostBackgroundStyle(
    val id: String,
    val isGradient: Boolean = false,
    val singleColor: Color = Color.Transparent,
    val gradientColors: List<Color> = emptyList(),
    val textColor: Color = Color(0xFF050505)
) {
    NONE("none", singleColor = Color.Transparent, textColor = Color(0xFF050505)),
    WHITE("color_white", singleColor = Color.White, textColor = Color(0xFF050505)),
    BLACK("color_black", singleColor = Color(0xFF18191A), textColor = Color.White),
    BLUE("color_blue", singleColor = Color(0xFF1877F2), textColor = Color.White),
    RED("color_red", singleColor = Color(0xFFB71C1C), textColor = Color.White),
    GREEN("color_green", singleColor = Color(0xFF1B5E20), textColor = Color.White),
    BG_SUNSET("bg_sunset", isGradient = true, gradientColors = listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), textColor = Color.White),
    BG_NIGHT("bg_night", isGradient = true, gradientColors = listOf(Color(0xFF2B1055), Color(0xFF7597DE)), textColor = Color.White),
    BG_OCEAN("bg_ocean", isGradient = true, gradientColors = listOf(Color(0xFF0052D4), Color(0xFF4364F7), Color(0xFF6FB1FC)), textColor = Color.White),
    BG_HILLS("bg_hills", isGradient = true, gradientColors = listOf(Color(0xFF134E5E), Color(0xFF71B280)), textColor = Color.White)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    userProfile: UserProfile?,
    mediaUploadService: MediaUploadService,
    onPostCreated: (PostItem) -> Unit,
    onBackClick: () -> Unit,
    onSubmitBackgroundPost: ((PostItem, List<Uri>, Uri?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var postText by remember { mutableStateOf("") }
    var selectedAudience by remember { mutableStateOf("Public") }
    var showAudienceSheet by remember { mutableStateOf(false) }
    var showHashtagSheet by remember { mutableStateOf(false) }
    var showStylePanel by remember { mutableStateOf(false) }
    var selectedBackground by remember { mutableStateOf(PostBackgroundStyle.NONE) }
    var fontSize by remember { mutableIntStateOf(24) }
    var textAlignState by remember { mutableStateOf(TextAlign.Start) }
    var mediaTypeState by remember { mutableStateOf("text") }

    val selectedMediaUris = remember { mutableStateListOf<Uri>() }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Multi-Photo Picker Activity Launcher (Up to 10 photos)
    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedMediaUris.clear()
            val safeUris = uris.take(10).map { uri ->
                val localPath = MediaUriHelper.copyUriToAppStorage(context, uri, "posts", "jpg")
                if (localPath.isNotBlank()) Uri.parse(localPath) else uri
            }
            selectedMediaUris.addAll(safeUris)
            selectedVideoUri = null
            mediaTypeState = "photo"
            selectedBackground = PostBackgroundStyle.NONE
            showStylePanel = false
        }
    }

    // Video / Reel Picker Activity Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = MediaUriHelper.copyUriToAppStorage(context, uri, "reels", "mp4")
            selectedVideoUri = if (localPath.isNotBlank()) Uri.parse(localPath) else uri
            selectedMediaUris.clear()
            selectedBackground = PostBackgroundStyle.NONE
            showStylePanel = false
        }
    }

    val displayName = when {
        !userProfile?.fullName.isNullOrBlank() -> userProfile?.fullName ?: "User"
        !userProfile?.firstName.isNullOrBlank() -> "${userProfile?.firstName} ${userProfile?.lastName}".trim()
        else -> "User"
    }
    val initial = displayName.firstOrNull()?.uppercase() ?: "U"

    val contentLimitManager = remember { ContentLimitManager.getInstance(context) }
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val appSettings by adminRepo.appSettingsFlow.collectAsState()

    val isPageProfile = userProfile?.uid?.startsWith("page_profile_") == true
    val pageId = if (isPageProfile) userProfile!!.uid.removePrefix("page_profile_") else ""
    val entityId = if (isPageProfile) pageId else (userProfile?.uid ?: "")

    val currentMediaType = when {
        mediaTypeState == "reel" -> "reel"
        mediaTypeState == "video" -> "video"
        selectedMediaUris.isNotEmpty() -> "photo"
        else -> "text"
    }

    val postValidation = remember(postText, selectedMediaUris.size, selectedVideoUri, mediaTypeState, appSettings, entityId) {
        contentLimitManager.validatePostCreation(
            entityId = entityId,
            isPage = isPageProfile,
            mediaType = currentMediaType,
            content = postText,
            mediaCount = if (currentMediaType == "photo") selectedMediaUris.size else if (currentMediaType == "video" || currentMediaType == "reel") 1 else 0,
            userProfile = userProfile
        )
    }
    val isPostBlocked = postValidation is ContentValidationResult.Blocked
    val postBlockReason = (postValidation as? ContentValidationResult.Blocked)?.reason ?: ""

    val hasBackground = selectedBackground != PostBackgroundStyle.NONE && selectedBackground != PostBackgroundStyle.WHITE
    val canPost = (postText.isNotBlank() || selectedMediaUris.isNotEmpty() || selectedVideoUri != null) && !isUploading && !isPostBlocked

    val isDarkMode = LocalIsDarkMode.current
    val bgColor = if (isDarkMode) Color(0xFF18191A) else Color.White
    val surfaceColor = if (isDarkMode) Color(0xFF242526) else Color(0xFFF9FAFB)
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val inputBg = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5)
    val dividerColor = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("create_post_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("create_post_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = if (mediaTypeState == "reel") "Create Reel" else "Create Post",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Button(
                    onClick = {
                        if (isPostBlocked) {
                            Toast.makeText(context, postBlockReason, Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (canPost) {
                            val finalMediaType = when {
                                mediaTypeState == "reel" -> "reel"
                                mediaTypeState == "video" -> "video"
                                selectedMediaUris.isNotEmpty() -> "photo"
                                else -> "text"
                            }

                            val isPageProfile = userProfile?.uid?.startsWith("page_profile_") == true
                            val pageId = if (isPageProfile) userProfile!!.uid.removePrefix("page_profile_") else ""

                            val postTemplate = PostItem(
                                authorId = userProfile?.uid ?: "user_id",
                                authorName = displayName,
                                authorAvatarUrl = userProfile?.profilePictureUrl ?: "",
                                content = postText.trim(),
                                mediaUrl = "",
                                mediaUrls = emptyList(),
                                mediaType = finalMediaType,
                                backgroundStyle = selectedBackground.id,
                                fontSize = fontSize,
                                textAlign = when (textAlignState) {
                                    TextAlign.Left -> "left"
                                    TextAlign.Right -> "right"
                                    else -> if (hasBackground) "center" else "left"
                                },
                                pageId = pageId,
                                audience = selectedAudience,
                                isAuthorVerified = (userProfile?.isVerificationActive() == true) || UserRepository.isUserVerifiedStatic(userProfile?.uid ?: "")
                            )

                            if (onSubmitBackgroundPost != null) {
                                onSubmitBackgroundPost(postTemplate, selectedMediaUris.toList(), selectedVideoUri)
                            } else {
                                isUploading = true
                                scope.launch {
                                    val uploadedUrls = mutableListOf<String>()

                                    if (mediaTypeState == "reel" || mediaTypeState == "video") {
                                        val uri = selectedVideoUri
                                        if (uri != null) {
                                            val res = mediaUploadService.uploadVideoUri(uri, folder = "reels")
                                            uploadedUrls.add(res.getOrDefault(uri.toString()))
                                        }
                                    } else if (selectedMediaUris.isNotEmpty()) {
                                        val uploadTasks = selectedMediaUris.map { uri ->
                                            async {
                                                val res = mediaUploadService.uploadImageUri(uri, folder = "posts")
                                                res.getOrDefault(uri.toString())
                                            }
                                        }
                                        uploadedUrls.addAll(uploadTasks.awaitAll())
                                    }

                                    val newPost = postTemplate.copy(
                                        mediaUrl = uploadedUrls.firstOrNull() ?: "",
                                        mediaUrls = uploadedUrls
                                    )

                                    isUploading = false
                                    onPostCreated(newPost)
                                }
                            }
                        }
                    },
                    enabled = canPost,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1877F2),
                        disabledContainerColor = Color(0xFF1877F2).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("submit_post_button")
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (mediaTypeState == "reel") "Share" else "Post",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Divider(thickness = 0.5.dp, color = dividerColor)

            // 2. User Row & Audience Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    shape = CircleShape,
                    color = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFD8DADF)
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
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        if (userProfile?.isVerificationActive() == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadge(size = 16.dp, show = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkMode) Color(0xFF263951) else Color(0xFFEBF3FE),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showAudienceSheet = true }
                            .testTag("post_audience_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (selectedAudience) {
                                    "Friends" -> Icons.Default.Group
                                    "Only Me" -> Icons.Default.Lock
                                    else -> Icons.Default.Public
                                },
                                contentDescription = selectedAudience,
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$selectedAudience ▾",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1877F2)
                            )
                        }
                    }
                }
            }

            // Warning Banner for Blocked or Limit-Exceeded Content
            if (isPostBlocked) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Limit Warning",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = postBlockReason,
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 3. Post Input / Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (hasBackground) 240.dp else 140.dp)
                    .padding(if (hasBackground) 0.dp else 16.dp)
                    .then(
                        if (hasBackground) {
                            if (selectedBackground.isGradient) {
                                Modifier.background(Brush.linearGradient(selectedBackground.gradientColors))
                            } else {
                                Modifier.background(selectedBackground.singleColor)
                            }
                        } else {
                            Modifier.background(bgColor)
                        }
                    ),
                contentAlignment = if (hasBackground) Alignment.Center else Alignment.TopStart
            ) {
                TextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = {
                        Text(
                            text = if (hasBackground) "Write something beautiful..." else if (mediaTypeState == "reel") "Write a caption for your reel..." else "What's on your mind?",
                            fontSize = if (hasBackground) fontSize.sp else 18.sp,
                            color = if (hasBackground) selectedBackground.textColor.copy(alpha = 0.65f) else textSecondary,
                            textAlign = if (hasBackground) TextAlign.Center else textAlignState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = if (hasBackground) fontSize.sp else 18.sp,
                        color = if (hasBackground) selectedBackground.textColor else textPrimary,
                        fontWeight = if (hasBackground) FontWeight.Bold else FontWeight.Normal,
                        textAlign = if (hasBackground) TextAlign.Center else textAlignState
                    ),
                    visualTransformation = remember(hasBackground, selectedBackground) {
                        HashtagVisualTransformation(
                            hashtagColor = if (hasBackground && selectedBackground.textColor == Color.White) {
                                Color(0xFF90CAF9)
                            } else {
                                Color(0xFF1877F2)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("create_post_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = if (hasBackground) selectedBackground.textColor else MaterialTheme.colorScheme.primary
                    )
                )
            }

            // 4. Multi-Photo Gallery Preview (Up to 10 photos)
            if (selectedMediaUris.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedMediaUris.size}/10 Photos Selected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1877F2)
                            )

                            if (selectedMediaUris.size < 10) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1877F2),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val photoValidation = contentLimitManager.validatePostCreation(
                                                entityId, isPageProfile, "photo", postText, selectedMediaUris.size + 1, userProfile
                                            )
                                            if (photoValidation is ContentValidationResult.Blocked) {
                                                Toast.makeText(context, photoValidation.reason, Toast.LENGTH_LONG).show()
                                            } else {
                                                multiPhotoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Add More",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Add More",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(selectedMediaUris) { index, uri ->
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Photo ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Remove Single Photo
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .clickable {
                                                selectedMediaUris.removeAt(index)
                                                if (selectedMediaUris.isEmpty()) {
                                                    mediaTypeState = "text"
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    // Badge number
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF1877F2),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                            .size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 11.sp,
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

            // 5. Video / Reel Preview (When single video is picked)
            if (selectedVideoUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Video Ready",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (mediaTypeState == "reel") "Reel video ready to upload" else "Video ready to upload",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Remove Video Button
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(34.dp)
                                .clickable {
                                    selectedVideoUri = null
                                    mediaTypeState = "text"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. Formatting Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Font Size Button "Tт 24"
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = inputBg,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            fontSize = when (fontSize) {
                                18 -> 24
                                24 -> 30
                                30 -> 36
                                else -> 18
                            }
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "Tт $fontSize",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = textPrimary
                        )
                    }
                }

                // Text Alignment Button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = inputBg,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            textAlignState = when (textAlignState) {
                                TextAlign.Start, TextAlign.Left -> TextAlign.Center
                                TextAlign.Center -> TextAlign.End
                                else -> TextAlign.Start
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (textAlignState) {
                                TextAlign.Start, TextAlign.Left -> Icons.Default.FormatAlignLeft
                                TextAlign.End, TextAlign.Right -> Icons.Default.FormatAlignRight
                                else -> Icons.Default.FormatAlignCenter
                            },
                            contentDescription = "Alignment",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Style Toggle Button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (showStylePanel) (if (isDarkMode) Color(0xFF263951) else Color(0xFFEBF3FE)) else inputBg,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showStylePanel = !showStylePanel }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Style",
                            tint = if (showStylePanel) Color(0xFF1877F2) else textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Style",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (showStylePanel) Color(0xFF1877F2) else textPrimary
                        )
                    }
                }

                if (hasBackground) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFFFEBEB),
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                selectedBackground = PostBackgroundStyle.NONE
                                showStylePanel = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFFFA383E),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFFA383E)
                            )
                        }
                    }
                }
            }

            // 7. Background Style Picker Card
            AnimatedVisibility(visible = showStylePanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Background Style",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        // Colors Section
                        Column {
                            Text(
                                text = "Colors",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(
                                    listOf(
                                        PostBackgroundStyle.NONE,
                                        PostBackgroundStyle.WHITE,
                                        PostBackgroundStyle.BLACK,
                                        PostBackgroundStyle.BLUE,
                                        PostBackgroundStyle.RED,
                                        PostBackgroundStyle.GREEN
                                    )
                                ) { bg ->
                                    val isSelected = selectedBackground == bg
                                    Surface(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF1877F2) else (if (isDarkMode) Color(0xFF4E4F50) else Color(0xFFCED0D4)),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedBackground = bg
                                                selectedMediaUris.clear()
                                                selectedVideoUri = null
                                            },
                                        color = if (bg == PostBackgroundStyle.NONE) (if (isDarkMode) Color(0xFF242526) else Color.White) else bg.singleColor
                                    ) {
                                        if (bg == PostBackgroundStyle.NONE) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "None",
                                                    tint = textSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Backgrounds Wallpapers Section
                        Column {
                            Text(
                                text = "Backgrounds",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(
                                    listOf(
                                        PostBackgroundStyle.BG_SUNSET,
                                        PostBackgroundStyle.BG_NIGHT,
                                        PostBackgroundStyle.BG_OCEAN,
                                        PostBackgroundStyle.BG_HILLS
                                    )
                                ) { bg ->
                                    val isSelected = selectedBackground == bg
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF1877F2) else (if (isDarkMode) Color(0xFF4E4F50) else Color(0xFFCED0D4)),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.linearGradient(bg.gradientColors))
                                            .clickable {
                                                selectedBackground = bg
                                                selectedMediaUris.clear()
                                                selectedVideoUri = null
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 8. Action Grid Card (Photos up to 10, Video, Reels, Tag)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Add Photos Button -> Pick up to 10 photos
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                val photoValidation = contentLimitManager.validatePostCreation(
                                    entityId, isPageProfile, "photo", postText, 1, userProfile
                                )
                                if (photoValidation is ContentValidationResult.Blocked) {
                                    Toast.makeText(context, photoValidation.reason, Toast.LENGTH_LONG).show()
                                } else {
                                    mediaTypeState = "photo"
                                    selectedBackground = PostBackgroundStyle.NONE
                                    multiPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            }
                            .testTag("action_add_photo")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Add Photos (up to 10)",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add Photos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    // 2. Add Video Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                val videoValidation = contentLimitManager.validatePostCreation(
                                    entityId, isPageProfile, "video", postText, 1, userProfile
                                )
                                if (videoValidation is ContentValidationResult.Blocked) {
                                    Toast.makeText(context, videoValidation.reason, Toast.LENGTH_LONG).show()
                                } else {
                                    mediaTypeState = "video"
                                    selectedBackground = PostBackgroundStyle.NONE
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                            }
                            .testTag("action_add_video")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Add Video",
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add Video",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    }

                    // 3. Reels Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                val reelValidation = contentLimitManager.validatePostCreation(
                                    entityId, isPageProfile, "reel", postText, 1, userProfile
                                )
                                if (reelValidation is ContentValidationResult.Blocked) {
                                    Toast.makeText(context, reelValidation.reason, Toast.LENGTH_LONG).show()
                                } else {
                                    mediaTypeState = "reel"
                                    selectedBackground = PostBackgroundStyle.NONE
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                            }
                            .testTag("action_add_reels")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF3E5F5),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SmartDisplay,
                                    contentDescription = "Reels",
                                    tint = Color(0xFF6A1B9A),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reels",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6A1B9A)
                        )
                    }

                    // 4. Tag Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { showHashtagSheet = true }
                            .testTag("action_add_tag")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE1F5FE),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = "Tag",
                                    tint = Color(0xFF0277BD),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tag",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0277BD)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // 8.5 Hashtag & Tag Modal Bottom Sheet
        if (showHashtagSheet) {
            HashtagPickerBottomSheet(
                currentText = postText,
                onHashtagSelected = { tag ->
                    val cleanTag = if (tag.startsWith("#")) tag else "#$tag"
                    postText = if (postText.isBlank()) {
                        "$cleanTag "
                    } else if (!postText.contains(cleanTag, ignoreCase = true)) {
                        "${postText.trim()} $cleanTag "
                    } else {
                        postText
                    }
                },
                onHashtagRemoved = { tag ->
                    val cleanTag = if (tag.startsWith("#")) tag else "#$tag"
                    postText = postText.replace(cleanTag, "", ignoreCase = true).replace(Regex("\\s+"), " ").trim()
                    if (postText.isNotEmpty()) postText = "$postText "
                },
                onDismiss = { showHashtagSheet = false }
            )
        }

        // 9. Audience Modal Bottom Sheet
        if (showAudienceSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAudienceSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = surfaceColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Who can see your post?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: Public
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedAudience = "Public"
                                showAudienceSheet = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkMode) Color(0xFF263951) else Color(0xFFEBF3FE),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Public",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Public",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Anyone on or off Frndom",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }

                        RadioButton(
                            selected = selectedAudience == "Public",
                            onClick = {
                                selectedAudience = "Public"
                                showAudienceSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                        )
                    }

                    // Option 2: Friends
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedAudience = "Friends"
                                showAudienceSheet = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkMode) Color(0xFF1E3A24) else Color(0xFFE8F5E9),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Friends",
                                    tint = if (isDarkMode) Color(0xFF4CAF50) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Friends",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Your friends on Frndom",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }

                        RadioButton(
                            selected = selectedAudience == "Friends",
                            onClick = {
                                selectedAudience = "Friends"
                                showAudienceSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                        )
                    }

                    // Option 3: Only Me
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedAudience = "Only Me"
                                showAudienceSheet = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkMode) Color(0xFF3E2D1A) else Color(0xFFFFF3E0),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Only Me",
                                    tint = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFEF6C00),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Only Me",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Only you can see this post",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }

                        RadioButton(
                            selected = selectedAudience == "Only Me",
                            onClick = {
                                selectedAudience = "Only Me"
                                showAudienceSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HashtagPickerBottomSheet(
    currentText: String,
    onHashtagSelected: (String) -> Unit,
    onHashtagRemoved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current
    val surfaceColor = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val inputBorder = if (isDarkMode) Color(0xFF4E4F50) else Color(0xFFCED0D4)
    val chipBg = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5)
    val chipBorder = if (isDarkMode) Color(0xFF4E4F50) else Color(0xFFE4E6EB)

    var customTagInput by remember { mutableStateOf("") }
    val customTags = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Hashtags & Tags",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Hashtag input + Add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customTagInput,
                    onValueChange = { customTagInput = it },
                    placeholder = {
                        Text(
                            text = "Custom tag (e.g. viral, tour)",
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1877F2),
                        unfocusedBorderColor = inputBorder,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_hashtag_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val clean = customTagInput.trim().removePrefix("#")
                        if (clean.isNotBlank()) {
                            val formatted = "#$clean"
                            if (!customTags.contains(formatted)) {
                                customTags.add(0, formatted)
                            }
                            onHashtagSelected(formatted)
                            customTagInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("add_custom_hashtag_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Popular Hashtags (Tap to insert)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Combined list: custom user-added hashtags + 24 default popular tags
            val allTags = remember(customTags.toList()) {
                (customTags + DEFAULT_POPULAR_HASHTAGS).distinct()
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    val isSelected = currentText.contains(tag, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (isDarkMode) Color(0xFF263951) else Color(0xFFE7F3FF)) else chipBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF1877F2) else chipBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (isSelected) {
                                    onHashtagRemoved(tag)
                                } else {
                                    onHashtagSelected(tag)
                                }
                            }
                            .testTag("hashtag_chip_$tag")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = tag,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1877F2) else textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("hashtag_done_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Text(
                    text = "Done",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
