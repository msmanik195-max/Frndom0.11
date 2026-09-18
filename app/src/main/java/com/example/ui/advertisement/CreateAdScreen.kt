package com.example.ui.advertisement

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AdvertisementItem
import com.example.data.model.PageItem
import com.example.data.model.UserProfile
import com.example.data.repository.AdvertisementRepository
import com.example.data.repository.AppSettingsRepository
import com.example.data.repository.GroupPageRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.WalletRepository
import com.example.util.MediaUriHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdScreen(
    currentUser: UserProfile?,
    onBack: () -> Unit,
    onAdCreated: (AdvertisementItem) -> Unit,
    onNavigateToDeposit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val walletRepo = remember { WalletRepository.getInstance(context) }
    val adRepo = remember { AdvertisementRepository.getInstance(context) }
    val appSettingsRepo = remember { AppSettingsRepository.getInstance(context) }
    val groupPageRepo = remember { GroupPageRepository.getInstance(context) }
    val postRepo = remember { PostRepository.getInstance(context) }
    val isDarkMode by appSettingsRepo.isDarkMode.collectAsState()

    val walletBalance by walletRepo.balanceFlow.collectAsState()
    val allPages by groupPageRepo.pagesFlow.collectAsState()
    val myPages = remember(allPages, currentUser) {
        val uid = currentUser?.uid.orEmpty()
        allPages.filter { it.creatorId == uid }
    }

    val userName = currentUser?.fullName.orEmpty().ifBlank {
        "${currentUser?.firstName} ${currentUser?.lastName}".trim()
    }.ifBlank { "Advertiser" }
    val userEmail = currentUser?.email.orEmpty()
    val userPhone = currentUser?.phoneNumber.orEmpty().ifBlank { currentUser?.contactPhone.orEmpty() }
    val userAvatar = currentUser?.profilePictureUrl.orEmpty()

    // Identity Selection: "PROFILE" vs "PAGE"
    var selectedPostType by remember { mutableStateOf("PROFILE") }
    var selectedPage by remember { mutableStateOf<PageItem?>(null) }

    // Media Type: "photo" or "video"
    var selectedMediaType by remember { mutableStateOf("photo") }
    var mediaUrl by remember { mutableStateOf("") }

    // Media Pickers using zero-permission photo/video picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = MediaUriHelper.copyUriToAppStorage(context, uri, "ad_media", "jpg")
            mediaUrl = if (localPath.isNotBlank()) localPath else uri.toString()
            selectedMediaType = "photo"
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = MediaUriHelper.copyUriToAppStorage(context, uri, "ad_media", "mp4")
            mediaUrl = if (localPath.isNotBlank()) localPath else uri.toString()
            selectedMediaType = "video"
        }
    }

    // Form fields
    var campaignName by remember { mutableStateOf("") }
    var selectedGoal by remember { mutableStateOf("Website Visits") }
    var headline by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var destinationUrl by remember { mutableStateOf("https://") }
    var selectedCta by remember { mutableStateOf("Learn More") }

    // Targeting
    var targetLocation by remember { mutableStateOf("All Bangladesh") }
    var targetAgeRange by remember { mutableStateOf("18 - 65+") }
    var targetGender by remember { mutableStateOf("All") }

    // Budget
    var dailyBudgetStr by remember { mutableStateOf("100") }
    var durationDays by remember { mutableIntStateOf(5) }

    var isSubmitting by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val dailyBudget = dailyBudgetStr.toDoubleOrNull() ?: 0.0
    val totalBudget = dailyBudget * durationDays
    val isBalanceSufficient = walletBalance >= totalBudget && totalBudget > 0

    val estimatedReachMin = (dailyBudget * 35 * durationDays).toInt().coerceAtLeast(100)
    val estimatedReachMax = (dailyBudget * 70 * durationDays).toInt().coerceAtLeast(200)
    val estimatedClicks = (dailyBudget * 2.8 * durationDays).toInt().coerceAtLeast(10)

    val goals = listOf(
        Triple("Website Visits", Icons.Default.Language, "Get more website clicks and traffic"),
        Triple("Page Promotion", Icons.Default.ThumbUp, "Boost page likes and followers"),
        Triple("Messages / Chat", Icons.Default.Chat, "Receive customer inquiries on WhatsApp/Messenger"),
        Triple("Post Engagement", Icons.Default.Favorite, "Increase post likes, comments, and shares"),
        Triple("Brand Awareness", Icons.Default.Campaign, "Maximize reach and brand visibility")
    )

    val ctaOptions = listOf("Learn More", "Shop Now", "Sign Up", "Contact Us", "Send Message", "Visit Page")

    val bgMain = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val cardBg = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val borderCol = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_ad_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Create Ad Campaign",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Sponsored Feeds & Reels Placement",
                            fontSize = 12.sp,
                            color = Color(0xFF1877F2)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("create_ad_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    // Current wallet balance chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF00C853).copy(alpha = 0.15f),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable(onClick = onNavigateToDeposit)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF008937),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BDT ${String.format(Locale.US, "%.0f", walletBalance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF008937)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardBg,
                    titleContentColor = textPrimary
                )
            )
        },
        bottomBar = {
            // Floating bottom bar to launch campaign
            Surface(
                color = cardBg,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Campaign Budget",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                            Text(
                                text = "BDT ${String.format(Locale.US, "%.2f", totalBudget)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1877F2)
                            )
                        }

                        Button(
                            onClick = {
                                if (campaignName.isBlank()) {
                                    Toast.makeText(context, "Please enter a Campaign Name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (headline.isBlank()) {
                                    Toast.makeText(context, "Please enter an Ad Headline", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (totalBudget <= 0) {
                                    Toast.makeText(context, "Budget must be greater than 0", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!isBalanceSufficient) {
                                    Toast.makeText(context, "Insufficient balance! Please recharge wallet.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                showConfirmDialog = true
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("launch_campaign_button")
                        ) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Publish Campaign",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgMain)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Campaign Objective / Goal
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TrackChanges, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Choose Campaign Goal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    goals.forEach { (goalName, icon, sub) ->
                        val isSelected = selectedGoal == goalName
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF1877F2).copy(alpha = 0.1f) else Color.Transparent,
                            border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1877F2))) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedGoal = goalName
                                    if (goalName == "Messages / Chat") {
                                        selectedCta = "Send Message"
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedGoal = goalName
                                        if (goalName == "Messages / Chat") {
                                            selectedCta = "Send Message"
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1877F2))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goalName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = sub,
                                        fontSize = 12.sp,
                                        color = textSecondary
                                    )
                                }
                                Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) Color(0xFF1877F2) else textSecondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // 2. Creative Details (Headline, Description, Image, CTA)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Ad Identity & Creative",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Promote As: Profile or Page
                    Text(
                        text = "Promote As (Identity)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedPostType == "PROFILE",
                            onClick = {
                                selectedPostType = "PROFILE"
                                selectedPage = null
                            },
                            label = { Text("Personal Profile ($userName)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1877F2),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                        if (myPages.isNotEmpty()) {
                            FilterChip(
                                selected = selectedPostType == "PAGE",
                                onClick = {
                                    selectedPostType = "PAGE"
                                    if (selectedPage == null) selectedPage = myPages.firstOrNull()
                                },
                                label = { Text("Page", maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1877F2),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }

                    if (selectedPostType == "PAGE" && myPages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select Page to Run Ad:",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(myPages) { page ->
                                val isPageSelected = selectedPage?.id == page.id
                                FilterChip(
                                    selected = isPageSelected,
                                    onClick = { selectedPage = page },
                                    label = { Text(page.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF008937),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campaign Name
                    OutlinedTextField(
                        value = campaignName,
                        onValueChange = { campaignName = it },
                        label = { Text("Campaign Name (e.g. Summer Sale 2026)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_campaign_name"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Headline
                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        label = { Text("Primary Headline") },
                        placeholder = { Text("e.g., Flat 50% Off On All Orders!") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_headline"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Primary Text / Description") },
                        placeholder = { Text("Describe your product, offer, or service to attract customers...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_description"),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Media Format Selection: Photo vs Video
                    Text(
                        text = "Ad Media Format",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedMediaType == "photo",
                            onClick = { selectedMediaType = "photo" },
                            label = { Text("Photo Ad") },
                            leadingIcon = { Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1877F2),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedMediaType == "video",
                            onClick = { selectedMediaType = "video" },
                            label = { Text("Video Ad (Reels & Feed)") },
                            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE65100),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Media Picker Button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedMediaType == "photo") {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Photo from Gallery", fontSize = 13.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Video from Gallery", fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Media / Banner Image URL
                    OutlinedTextField(
                        value = mediaUrl,
                        onValueChange = { mediaUrl = it },
                        label = { Text(if (selectedMediaType == "video") "Video URL (or selected above)" else "Photo URL (or selected above)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_media_url"),
                        singleLine = true,
                        placeholder = { Text("https://...") }
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Destination URL or In-App Messenger Chat Connection
                    if (selectedCta == "Send Message") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1877F2).copy(alpha = 0.08f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1877F2).copy(alpha = 0.35f))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1877F2),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "In-App Messenger Chat Connected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1877F2)
                                    )
                                    Text(
                                        text = "গ্রাহকরা 'Send Message' বাটনে ক্লিক করলেই সরাসরি আপনার ফ্রেনডম ইন-অ্যাপ চ্যাটে মেসেজ করতে পারবে। কোনো ওয়েবসাইট লিংকের প্রয়োজন নেই।",
                                        fontSize = 11.sp,
                                        color = textSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = destinationUrl,
                            onValueChange = { destinationUrl = it },
                            label = { Text("Destination URL (Website or Page Link)") },
                            placeholder = { Text("https://yourwebsite.com or page link") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_destination_url"),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Call to Action (CTA)
                    Text(
                        text = "Call to Action (CTA Button)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ctaOptions) { cta ->
                            val isSelected = selectedCta == cta
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCta = cta },
                                label = { Text(cta, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1877F2),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 3. Live Feed Ad Mockup Preview
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF008937), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Ad Preview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Feed Ad Card Mockup
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkMode) Color(0xFF1C1E21) else Color(0xFFFAFAFA),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(borderCol)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header: Avatar, Name, Sponsored
                            val activePage = selectedPage
                            val previewName = if (selectedPostType == "PAGE" && activePage != null) activePage.name else userName
                            val previewAvatar = if (selectedPostType == "PAGE" && activePage != null) activePage.avatarUrl else userAvatar

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1877F2),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    if (previewAvatar.isNotBlank()) {
                                        AsyncImage(
                                            model = previewAvatar,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = previewName.firstOrNull()?.uppercase() ?: "A",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = previewName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textPrimary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Sponsored • ",
                                            fontSize = 11.sp,
                                            color = textSecondary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = textSecondary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // Description
                            Text(
                                text = description.ifBlank { "Your exciting promotional message will appear here for all viewers on the news feed." },
                                fontSize = 13.sp,
                                color = textPrimary,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Ad Banner Image / Video Preview
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (mediaUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = mediaUrl,
                                            contentDescription = "Ad Creative",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = if (selectedMediaType == "video") Icons.Default.Videocam else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (selectedMediaType == "video") "Video Ad Preview" else "Photo Ad Preview",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    if (selectedMediaType == "video") {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play Video",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom Headline & CTA Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = if (selectedCta == "Send Message") "MESSENGER CHAT" else destinationUrl.removePrefix("https://").take(25).ifBlank { "frndom.app" },
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = headline.ifBlank { "Your Headline Here" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1877F2),
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = selectedCta,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Audience Targeting
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = Color(0xFF673AB7), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3. Target Audience",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Location
                    Text(text = "Location", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    val locations = listOf("All Bangladesh", "Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(locations) { loc ->
                            FilterChip(
                                selected = targetLocation == loc,
                                onClick = { targetLocation = loc },
                                label = { Text(loc) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Age Range
                    Text(text = "Age Group", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    val ages = listOf("18 - 65+", "18 - 35", "25 - 45", "30 - 55")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ages) { age ->
                            FilterChip(
                                selected = targetAgeRange == age,
                                onClick = { targetAgeRange = age },
                                label = { Text(age) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gender
                    Text(text = "Gender", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Men", "Women").forEach { g ->
                            FilterChip(
                                selected = targetGender == g,
                                onClick = { targetGender = g },
                                label = { Text(g) }
                            )
                        }
                    }
                }
            }

            // 5. Budget & Duration
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = Color(0xFF008937), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "4. Budget & Schedule",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily Budget
                    Text(
                        text = "Daily Budget: BDT $dailyBudgetStr",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val budgetPresets = listOf("50", "100", "200", "500", "1000")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(budgetPresets) { preset ->
                            FilterChip(
                                selected = dailyBudgetStr == preset,
                                onClick = { dailyBudgetStr = preset },
                                label = { Text("BDT $preset/day") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dailyBudgetStr,
                        onValueChange = { dailyBudgetStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Custom Daily Budget (BDT)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Duration Days
                    Text(
                        text = "Duration: $durationDays Days",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val durationPresets = listOf(1, 3, 5, 7, 15, 30)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(durationPresets) { days ->
                            FilterChip(
                                selected = durationDays == days,
                                onClick = { durationDays = days },
                                label = { Text("$days Days") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Estimation Metrics Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1877F2).copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Estimated Performance:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1877F2)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Total Estimated Reach", fontSize = 11.sp, color = textSecondary)
                                    Text(
                                        text = "$estimatedReachMin - $estimatedReachMax people",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Estimated Link Clicks", fontSize = 11.sp, color = textSecondary)
                                    Text(
                                        text = "~$estimatedClicks clicks",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Main Wallet Balance & Deduction Summary
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF008937), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "5. Main Wallet Payment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Current Wallet Balance:", fontSize = 14.sp, color = textSecondary)
                        Text(
                            text = "BDT ${String.format(Locale.US, "%.2f", walletBalance)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Campaign Cost (${durationDays}d × BDT $dailyBudget):", fontSize = 14.sp, color = textSecondary)
                        Text(
                            text = "- BDT ${String.format(Locale.US, "%.2f", totalBudget)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = borderCol)

                    val balanceAfter = walletBalance - totalBudget
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Balance Remaining After:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Text(
                            text = "BDT ${String.format(Locale.US, "%.2f", balanceAfter)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (balanceAfter >= 0) Color(0xFF008937) else Color(0xFFE53935)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isBalanceSufficient) {
                        // Warning & Recharge Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFDECEA),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE53935))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Insufficient Wallet Balance!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your Main Wallet has insufficient funds. You need BDT ${String.format(Locale.US, "%.2f", totalBudget - walletBalance)} more. Please deposit to run ads.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5D101D)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateToDeposit,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Recharge Wallet Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        // Balance OK Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Wallet balance is sufficient. BDT ${String.format(Locale.US, "%.2f", totalBudget)} will be deducted upon submission.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Ad Campaign",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to launch \"$campaignName\"?",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "BDT ${String.format(Locale.US, "%.2f", totalBudget)} will be deducted from your Main Wallet. Your ad will be reviewed by admin and start running.",
                        fontSize = 13.sp,
                        color = Color(0xFF65676B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isSubmitting = true

                        val activePage = selectedPage
                        val effectiveUserName = if (selectedPostType == "PAGE" && activePage != null) activePage.name else userName
                        val effectiveAvatar = if (selectedPostType == "PAGE" && activePage != null) activePage.avatarUrl else userAvatar

                        val newAd = AdvertisementItem(
                            userId = currentUser?.uid.orEmpty(),
                            userName = effectiveUserName,
                            userEmail = userEmail,
                            userPhone = userPhone,
                            userAvatar = effectiveAvatar,
                            campaignName = campaignName.trim(),
                            campaignGoal = selectedGoal,
                            headline = headline.trim(),
                            description = description.trim(),
                            mediaUrl = mediaUrl.trim(),
                            destinationUrl = if (selectedCta == "Send Message") "chat:${currentUser?.uid.orEmpty()}" else destinationUrl.trim(),
                            callToAction = selectedCta,
                            targetLocation = targetLocation,
                            targetAgeRange = targetAgeRange,
                            targetGender = targetGender,
                            dailyBudget = dailyBudget,
                            durationDays = durationDays,
                            totalBudget = totalBudget,
                            status = "PENDING",
                            createdAt = System.currentTimeMillis(),
                            estimatedReach = estimatedReachMax,
                            estimatedClicks = estimatedClicks,
                            mediaType = selectedMediaType,
                            postType = selectedPostType,
                            pageId = if (selectedPostType == "PAGE") selectedPage?.id.orEmpty() else "",
                            pageName = if (selectedPostType == "PAGE") selectedPage?.name.orEmpty() else "",
                            pageAvatarUrl = if (selectedPostType == "PAGE") selectedPage?.avatarUrl.orEmpty() else ""
                        )

                        val result = adRepo.submitAdvertisement(newAd, walletRepo, postRepo)
                        isSubmitting = false

                        if (result.isSuccess) {
                            Toast.makeText(context, "Advertisement submitted successfully! BDT ${String.format(Locale.US, "%.2f", totalBudget)} deducted.", Toast.LENGTH_LONG).show()
                            onAdCreated(result.getOrThrow())
                        } else {
                            Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed to submit ad", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Text("Confirm & Pay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
