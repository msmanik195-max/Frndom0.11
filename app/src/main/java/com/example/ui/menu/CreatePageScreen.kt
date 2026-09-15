package com.example.ui.menu

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.ui.components.ImageCropDialog
import com.example.ui.components.CropShape
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PageItem
import com.example.data.model.UserProfile
import com.example.data.repository.GroupPageRepository
import com.example.data.service.MediaUploadService
import com.example.ui.theme.LocalIsDarkMode
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CreatePageScreen(
    userProfile: UserProfile?,
    groupPageRepository: GroupPageRepository,
    mediaUploadService: MediaUploadService,
    pageToEdit: PageItem? = null,
    onPageCreated: (PageItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = pageToEdit != null
    val scope = rememberCoroutineScope()

    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)
    val inputContainerColor = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5)
    val circleBg = if (isDarkMode) Color(0xFF263951) else Color(0xFFEBF5FF)
    val bannerEmptyBg = if (isDarkMode) Color(0xFF2A2B2D) else Color(0xFFE4E6EB)

    var pageName by remember { mutableStateOf(pageToEdit?.name.orEmpty()) }
    var pageCategory by remember { mutableStateOf(pageToEdit?.category ?: "Digital Creator") }
    var pageDescription by remember { mutableStateOf(pageToEdit?.description.orEmpty()) }
    var pagePrivacy by remember { mutableStateOf(if (pageToEdit?.id?.isNotBlank() == true) "Public" else "Public") }

    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }

    var currentAvatarUrl by remember { mutableStateOf(pageToEdit?.avatarUrl.orEmpty()) }
    var currentCoverUrl by remember { mutableStateOf(pageToEdit?.coverUrl.orEmpty()) }

    var rawAvatarUriToCrop by remember { mutableStateOf<Uri?>(null) }
    var rawCoverUriToCrop by remember { mutableStateOf<Uri?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }

    val categories = listOf(
        "Digital Creator",
        "Business & Brand",
        "Community & Club",
        "Gaming & Esports",
        "Education & Learning",
        "Entertainment & Art",
        "News & Media",
        "Health & Fitness"
    )

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            rawAvatarUriToCrop = uri
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            rawCoverUriToCrop = uri
        }
    }

    val context = LocalContext.current
    val contentLimitManager = remember { ContentLimitManager.getInstance(context) }
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val appSettings by adminRepo.appSettingsFlow.collectAsState()

    val pageCreationValidation = remember(appSettings, isEditMode) {
        if (isEditMode) ContentValidationResult.Allowed else contentLimitManager.validatePageCreation()
    }
    val isPageCreationBlocked = pageCreationValidation is ContentValidationResult.Blocked
    val pageCreationBlockReason = (pageCreationValidation as? ContentValidationResult.Blocked)?.reason ?: ""

    val isValid = pageName.trim().length >= 2 && !isSubmitting && !isPageCreationBlocked

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgScreen)
            .testTag("create_page_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bgCard,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditMode) "Edit Page" else "Create a Page",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Button(
                        onClick = {
                            if (isPageCreationBlocked) {
                                Toast.makeText(context, pageCreationBlockReason, Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (isValid) {
                                isSubmitting = true
                                scope.launch {
                                    var finalAvatar = currentAvatarUrl
                                    var finalCover = currentCoverUrl

                                    avatarUri?.let { uri ->
                                        val res = mediaUploadService.uploadImageUri(uri, folder = "pages/avatars")
                                        finalAvatar = res.getOrDefault(uri.toString())
                                    }

                                    coverUri?.let { uri ->
                                        val res = mediaUploadService.uploadImageUri(uri, folder = "pages/covers")
                                        finalCover = res.getOrDefault(uri.toString())
                                    }

                                    val savedPage = if (isEditMode) {
                                        val updated = pageToEdit!!.copy(
                                            name = pageName.trim(),
                                            category = pageCategory,
                                            description = pageDescription.trim(),
                                            avatarUrl = finalAvatar,
                                            coverUrl = finalCover
                                        )
                                        groupPageRepository.updatePage(updated)
                                        updated
                                    } else {
                                        val newPage = PageItem(
                                            id = "page_" + UUID.randomUUID().toString().take(8),
                                            name = pageName.trim(),
                                            category = pageCategory,
                                            description = pageDescription.trim(),
                                            coverUrl = finalCover,
                                            avatarUrl = finalAvatar,
                                            creatorId = userProfile?.uid ?: "user",
                                            followersCount = 1,
                                            likesCount = 1,
                                            createdAt = System.currentTimeMillis()
                                        )
                                        groupPageRepository.createPage(newPage)
                                        newPage
                                    }

                                    isSubmitting = false
                                    onPageCreated(savedPage)
                                }
                            }
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            disabledContainerColor = Color(0xFF1877F2).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("submit_page_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isEditMode) "Save" else "Create",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isPageCreationBlocked) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = pageCreationBlockReason,
                                color = Color(0xFFC62828),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Section 1: Page Visual Branding (Banner & Profile Picture)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Page Branding",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            modifier = Modifier.padding(14.dp)
                        )

                        Divider(thickness = 0.5.dp, color = dividerColor)

                        // Cover Photo Picker Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(bannerEmptyBg)
                                .clickable {
                                    coverPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverUri != null) {
                                AsyncImage(
                                    model = coverUri,
                                    contentDescription = "Cover Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (currentCoverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = currentCoverUrl,
                                    contentDescription = "Cover Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = textSecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add Page Cover Banner",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSecondary
                                    )
                                }
                            }

                            // Camera button overlay on top right of banner
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(36.dp),
                                shape = CircleShape,
                                color = bgCard.copy(alpha = 0.9f),
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change Cover",
                                        tint = textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Avatar Section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF1877F2), CircleShape)
                                    .clickable {
                                        avatarPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUri != null) {
                                    AsyncImage(
                                        model = avatarUri,
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (currentAvatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = currentAvatarUrl,
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = circleBg
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = Color(0xFF1877F2),
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF1877F2)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Upload Avatar",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Page Profile Picture",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Tap circle to choose an icon or photo",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                // Section 2: Page Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Page Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        // Page Name
                        OutlinedTextField(
                            value = pageName,
                            onValueChange = { pageName = it },
                            label = { Text("Page Name *", color = textSecondary) },
                            placeholder = { Text("e.g. Awesome Tech Creators", color = textSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("page_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = dividerColor,
                                focusedLabelColor = Color(0xFF1877F2),
                                unfocusedLabelColor = textSecondary
                            )
                        )

                        // Description / Bio
                        OutlinedTextField(
                            value = pageDescription,
                            onValueChange = { pageDescription = it },
                            label = { Text("Description & Bio", color = textSecondary) },
                            placeholder = { Text("Tell people what your Page is about...", color = textSecondary) },
                            shape = RoundedCornerShape(8.dp),
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("page_desc_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = dividerColor,
                                focusedLabelColor = Color(0xFF1877F2),
                                unfocusedLabelColor = textSecondary
                            )
                        )
                    }
                }

                // Section 3: Category Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Select Category",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A category helps people discover your page easily",
                            fontSize = 12.sp,
                            color = textSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.chunked(2).forEach { rowCategories ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowCategories.forEach { cat ->
                                        val isSelected = pageCategory == cat
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { pageCategory = cat },
                                            label = {
                                                Text(
                                                    text = cat,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) (if (isDarkMode) Color(0xFF4599FF) else Color(0xFF1877F2)) else textPrimary
                                                )
                                            },
                                            leadingIcon = if (isSelected) {
                                                { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDarkMode) Color(0xFF4599FF) else Color(0xFF1877F2)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = circleBg,
                                                selectedLabelColor = Color(0xFF1877F2),
                                                containerColor = bgCard
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowCategories.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Public / Private Visibility Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgCard),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Page Visibility",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Public Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pagePrivacy = "Public" }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = pagePrivacy == "Public",
                                onClick = { pagePrivacy = "Public" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Public Page", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                                Text("Anyone on or off Frndom can see your page and posts", fontSize = 12.sp, color = textSecondary)
                            }
                        }

                        // Private Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pagePrivacy = "Private" }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = pagePrivacy == "Private",
                                onClick = { pagePrivacy = "Private" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Private Page", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                                Text("Only approved followers can see what you post", fontSize = 12.sp, color = textSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Avatar Cropping Dialog
        rawAvatarUriToCrop?.let { uriToCrop ->
            ImageCropDialog(
                imageUri = uriToCrop,
                cropShape = CropShape.CIRCLE,
                title = "Crop Page Profile Picture",
                onCropSuccess = { croppedUri ->
                    avatarUri = croppedUri
                    rawAvatarUriToCrop = null
                },
                onDismiss = { rawAvatarUriToCrop = null }
            )
        }

        // Cover Cropping Dialog
        rawCoverUriToCrop?.let { uriToCrop ->
            ImageCropDialog(
                imageUri = uriToCrop,
                cropShape = CropShape.COVER,
                title = "Crop Page Cover Photo",
                onCropSuccess = { croppedUri ->
                    coverUri = croppedUri
                    rawCoverUriToCrop = null
                },
                onDismiss = { rawCoverUriToCrop = null }
            )
        }
    }
}
