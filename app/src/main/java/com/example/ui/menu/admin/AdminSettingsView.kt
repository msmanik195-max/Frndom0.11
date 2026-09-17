package com.example.ui.menu.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSystemSettings
import com.example.data.repository.AdminRequestRepository
import com.example.ui.theme.LocalIsDarkMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsView(
    adminRepo: AdminRequestRepository,
    onBack: () -> Unit,
    onFeedCustomizationClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)
    val inputBg = if (isDarkMode) Color(0xFF3A3B3C) else Color.White

    val currentMonetization by adminRepo.monetizationSettingsFlow.collectAsState()
    val currentAppSettings by adminRepo.appSettingsFlow.collectAsState()

    // App Feature Toggles State
    var reqIdCard by remember { mutableStateOf(currentAppSettings.requireIdCardForVerification) }
    var monetizationEnabled by remember { mutableStateOf(currentAppSettings.monetizationEnabled) }
    var depositEnabled by remember { mutableStateOf(currentAppSettings.depositEnabled) }
    var withdrawEnabled by remember { mutableStateOf(currentAppSettings.withdrawEnabled) }
    var textPostEnabled by remember { mutableStateOf(currentAppSettings.textPostEnabled) }
    var imagePostEnabled by remember { mutableStateOf(currentAppSettings.imagePostEnabled) }
    var videoPostEnabled by remember { mutableStateOf(currentAppSettings.videoPostEnabled) }
    var storyPostEnabled by remember { mutableStateOf(currentAppSettings.storyPostEnabled) }
    var linkPostEnabled by remember { mutableStateOf(currentAppSettings.linkPostEnabled) }
    var pageCreationEnabled by remember { mutableStateOf(currentAppSettings.pageCreationEnabled) }
    var groupCreationEnabled by remember { mutableStateOf(currentAppSettings.groupCreationEnabled) }
    var engagementNotifEnabled by remember { mutableStateOf(currentAppSettings.engagementNotificationsEnabled) }

    var maxVideoMinutes by remember { mutableStateOf(currentAppSettings.maxVideoDurationMinutes.toString()) }
    var userTextLimit by remember { mutableStateOf(currentAppSettings.defaultUserDailyLimitText.toString()) }
    var userImageLimit by remember { mutableStateOf(currentAppSettings.defaultUserDailyLimitImage.toString()) }
    var userVideoLimit by remember { mutableStateOf(currentAppSettings.defaultUserDailyLimitVideo.toString()) }
    var userStoryLimit by remember { mutableStateOf(currentAppSettings.defaultUserDailyLimitStory.toString()) }
    var userLinkLimit by remember { mutableStateOf(currentAppSettings.defaultUserDailyLimitLink.toString()) }

    var pageTextLimit by remember { mutableStateOf(currentAppSettings.defaultPageDailyLimitText.toString()) }
    var pageImageLimit by remember { mutableStateOf(currentAppSettings.defaultPageDailyLimitImage.toString()) }
    var pageVideoLimit by remember { mutableStateOf(currentAppSettings.defaultPageDailyLimitVideo.toString()) }
    var pageStoryLimit by remember { mutableStateOf(currentAppSettings.defaultPageDailyLimitStory.toString()) }
    var pageLinkLimit by remember { mutableStateOf(currentAppSettings.defaultPageDailyLimitLink.toString()) }
    var leaderboardLimit by remember { mutableStateOf(currentAppSettings.leaderboardLimit.toString()) }

    // Monetization Rates State
    var reelRate by remember { mutableStateOf(currentMonetization.reelRatePer1000.toString()) }
    var imageRate by remember { mutableStateOf(currentMonetization.imageRatePer1000.toString()) }
    var textRate by remember { mutableStateOf(currentMonetization.textRatePer1000.toString()) }
    var minTransfer by remember { mutableStateOf(currentMonetization.minTransferAmount.toString()) }
    var reqViews by remember { mutableStateOf(currentMonetization.reqTotalViews.toString()) }
    var reqFollowers by remember { mutableStateOf(currentMonetization.reqTotalFollowers.toString()) }
    var reqPosts by remember { mutableStateOf(currentMonetization.reqTotalPosts.toString()) }
    var reqReels by remember { mutableStateOf(currentMonetization.reqTotalReels.toString()) }
    var reqAge by remember { mutableStateOf(currentMonetization.reqAccountAgeDays.toString()) }

    // Story Expiry Config State (Hours, Minutes, Seconds)
    val currentStoryExpiry by adminRepo.storyExpiryConfigFlow.collectAsState()
    var storyExpiryHours by remember { mutableStateOf(currentStoryExpiry.hours.toString()) }
    var storyExpiryMinutes by remember { mutableStateOf(currentStoryExpiry.minutes.toString()) }
    var storyExpirySeconds by remember { mutableStateOf(currentStoryExpiry.seconds.toString()) }

    LaunchedEffect(currentStoryExpiry) {
        storyExpiryHours = currentStoryExpiry.hours.toString()
        storyExpiryMinutes = currentStoryExpiry.minutes.toString()
        storyExpirySeconds = currentStoryExpiry.seconds.toString()
    }

    LaunchedEffect(currentAppSettings) {
        reqIdCard = currentAppSettings.requireIdCardForVerification
        monetizationEnabled = currentAppSettings.monetizationEnabled
        depositEnabled = currentAppSettings.depositEnabled
        withdrawEnabled = currentAppSettings.withdrawEnabled
        textPostEnabled = currentAppSettings.textPostEnabled
        imagePostEnabled = currentAppSettings.imagePostEnabled
        videoPostEnabled = currentAppSettings.videoPostEnabled
        storyPostEnabled = currentAppSettings.storyPostEnabled
        linkPostEnabled = currentAppSettings.linkPostEnabled
        pageCreationEnabled = currentAppSettings.pageCreationEnabled
        groupCreationEnabled = currentAppSettings.groupCreationEnabled
        engagementNotifEnabled = currentAppSettings.engagementNotificationsEnabled
        maxVideoMinutes = currentAppSettings.maxVideoDurationMinutes.toString()
        userTextLimit = currentAppSettings.defaultUserDailyLimitText.toString()
        userImageLimit = currentAppSettings.defaultUserDailyLimitImage.toString()
        userVideoLimit = currentAppSettings.defaultUserDailyLimitVideo.toString()
        userStoryLimit = currentAppSettings.defaultUserDailyLimitStory.toString()
        userLinkLimit = currentAppSettings.defaultUserDailyLimitLink.toString()
        pageTextLimit = currentAppSettings.defaultPageDailyLimitText.toString()
        pageImageLimit = currentAppSettings.defaultPageDailyLimitImage.toString()
        pageVideoLimit = currentAppSettings.defaultPageDailyLimitVideo.toString()
        pageStoryLimit = currentAppSettings.defaultPageDailyLimitStory.toString()
        pageLinkLimit = currentAppSettings.defaultPageDailyLimitLink.toString()
        leaderboardLimit = currentAppSettings.leaderboardLimit.toString()
    }

    LaunchedEffect(currentMonetization) {
        reelRate = currentMonetization.reelRatePer1000.toString()
        imageRate = currentMonetization.imageRatePer1000.toString()
        textRate = currentMonetization.textRatePer1000.toString()
        minTransfer = currentMonetization.minTransferAmount.toString()
        reqViews = currentMonetization.reqTotalViews.toString()
        reqFollowers = currentMonetization.reqTotalFollowers.toString()
        reqPosts = currentMonetization.reqTotalPosts.toString()
        reqReels = currentMonetization.reqTotalReels.toString()
        reqAge = currentMonetization.reqAccountAgeDays.toString()
    }

    val scrollState = rememberScrollState()

    fun saveAllSettings(silent: Boolean = false) {
        val updatedAppSettings = AppSystemSettings(
            requireIdCardForVerification = reqIdCard,
            monetizationEnabled = monetizationEnabled,
            depositEnabled = depositEnabled,
            withdrawEnabled = withdrawEnabled,
            textPostEnabled = textPostEnabled,
            imagePostEnabled = imagePostEnabled,
            videoPostEnabled = videoPostEnabled,
            storyPostEnabled = storyPostEnabled,
            linkPostEnabled = linkPostEnabled,
            pageCreationEnabled = pageCreationEnabled,
            groupCreationEnabled = groupCreationEnabled,
            engagementNotificationsEnabled = engagementNotifEnabled,
            maxVideoDurationMinutes = maxVideoMinutes.toIntOrNull() ?: 10,
            defaultUserDailyLimitText = userTextLimit.toIntOrNull() ?: 10,
            defaultUserDailyLimitImage = userImageLimit.toIntOrNull() ?: 10,
            defaultUserDailyLimitVideo = userVideoLimit.toIntOrNull() ?: 5,
            defaultUserDailyLimitStory = userStoryLimit.toIntOrNull() ?: 10,
            defaultUserDailyLimitLink = userLinkLimit.toIntOrNull() ?: 10,
            defaultPageDailyLimitText = pageTextLimit.toIntOrNull() ?: 20,
            defaultPageDailyLimitImage = pageImageLimit.toIntOrNull() ?: 20,
            defaultPageDailyLimitVideo = pageVideoLimit.toIntOrNull() ?: 10,
            defaultPageDailyLimitStory = pageStoryLimit.toIntOrNull() ?: 20,
            defaultPageDailyLimitLink = pageLinkLimit.toIntOrNull() ?: 20,
            leaderboardLimit = leaderboardLimit.toIntOrNull()?.coerceAtLeast(3) ?: 20,
            updatedAt = System.currentTimeMillis()
        )
        adminRepo.updateAppSettings(updatedAppSettings)

        adminRepo.setMonetizationSettings(
            reelRate = reelRate.toDoubleOrNull() ?: 0.5,
            imageRate = imageRate.toDoubleOrNull() ?: 0.2,
            textRate = textRate.toDoubleOrNull() ?: 0.1,
            minTransfer = minTransfer.toDoubleOrNull() ?: 5.0,
            reqViews = reqViews.toIntOrNull() ?: 500,
            reqFollowers = reqFollowers.toIntOrNull() ?: 100,
            reqPosts = reqPosts.toIntOrNull() ?: 10,
            reqReels = reqReels.toIntOrNull() ?: 5,
            reqAge = reqAge.toIntOrNull() ?: 7
        )

        val updatedStoryExpiry = com.example.data.model.StoryExpiryConfig(
            hours = storyExpiryHours.toIntOrNull() ?: 24,
            minutes = storyExpiryMinutes.toIntOrNull() ?: 0,
            seconds = storyExpirySeconds.toIntOrNull() ?: 0
        )
        adminRepo.updateStoryExpiryConfig(updatedStoryExpiry)

        if (!engagementNotifEnabled) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.example.data.repository.NotificationRepository.getInstance(context).purgeEngagementNotifications()
            }
        }

        if (!silent) {
            Toast.makeText(context, "All Admin Settings updated successfully!", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    fun saveStoryExpiry(silent: Boolean = false) {
        val updatedStoryExpiry = com.example.data.model.StoryExpiryConfig(
            hours = storyExpiryHours.toIntOrNull() ?: 24,
            minutes = storyExpiryMinutes.toIntOrNull() ?: 0,
            seconds = storyExpirySeconds.toIntOrNull() ?: 0
        )
        adminRepo.updateStoryExpiryConfig(updatedStoryExpiry)
        if (!silent) {
            Toast.makeText(context, "Story auto-delete duration updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
                        Text("System features, limits & policies", fontSize = 12.sp, color = textSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { saveAllSettings() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF1877F2))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgCard,
                    titleContentColor = textPrimary
                )
            )
        },
        containerColor = bgScreen,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { saveAllSettings() },
                icon = { Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White) },
                text = { Text("Save Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                containerColor = Color(0xFF1877F2),
                modifier = Modifier.testTag("admin_save_settings_btn")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgScreen)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: VERIFIED ID CARD REQUIREMENT (Requested at the very top)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("setting_id_card_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Verified ID Card Requirement",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = if (reqIdCard) "Required (Front & Back image)" else "Optional (Form only)",
                                    fontSize = 12.sp,
                                    color = if (reqIdCard) Color(0xFF00C853) else textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = reqIdCard,
                            onCheckedChange = {
                                reqIdCard = it
                                saveAllSettings(silent = true)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00C853)
                            ),
                            modifier = Modifier.testTag("setting_toggle_id_card")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When ON, users must upload both Front and Back photos of their National ID card to submit verification requests. When OFF, users can request verification without ID photos.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // SECTION 2: GLOBAL CORE FEATURE TOGGLES
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "System Core Feature Controls",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1877F2)
                    )

                    // Monetization Toggle
                    SettingToggleRow(
                        title = "Monetization Program",
                        subtitle = "Enable or disable creator monetization across the app",
                        icon = Icons.Default.MonetizationOn,
                        iconTint = Color(0xFFFFA000),
                        checked = monetizationEnabled,
                        onCheckedChange = {
                            monetizationEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Deposit Toggle
                    SettingToggleRow(
                        title = "Deposit Wallet Feature",
                        subtitle = "Allow users to submit money deposits to their balance",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = Color(0xFF2E7D32),
                        checked = depositEnabled,
                        onCheckedChange = {
                            depositEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Withdraw Toggle
                    SettingToggleRow(
                        title = "Withdraw Wallet Feature",
                        subtitle = "Allow users to withdraw earnings to mobile banking",
                        icon = Icons.Default.AccountBalance,
                        iconTint = Color(0xFF1565C0),
                        checked = withdrawEnabled,
                        onCheckedChange = {
                            withdrawEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Page Creation Toggle
                    SettingToggleRow(
                        title = "Page Creation",
                        subtitle = "Allow users to create new public creator & brand pages",
                        icon = Icons.Default.Flag,
                        iconTint = Color(0xFFE91E63),
                        checked = pageCreationEnabled,
                        onCheckedChange = {
                            pageCreationEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Group Creation Toggle
                    SettingToggleRow(
                        title = "Group Creation",
                        subtitle = "Allow users to create new community groups",
                        icon = Icons.Default.GroupAdd,
                        iconTint = Color(0xFF8E24AA),
                        checked = groupCreationEnabled,
                        onCheckedChange = {
                            groupCreationEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Engagement Notifications Toggle
                    SettingToggleRow(
                        title = "Social Engagement Notifications",
                        subtitle = "Send notifications for Likes and Comments. If disabled, past like/comment notifications will also be cleared.",
                        icon = Icons.Default.NotificationsActive,
                        iconTint = Color(0xFF0097A7),
                        checked = engagementNotifEnabled,
                        onCheckedChange = { isChecked ->
                            engagementNotifEnabled = isChecked
                            saveAllSettings(silent = true)
                            if (!isChecked) {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    com.example.data.repository.NotificationRepository.getInstance(context).purgeEngagementNotifications()
                                }
                            }
                        }
                    )
                }
            }

            // SECTION 3: POSTING PERMISSIONS & VIDEO DURATION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Content Creation & Upload Controls",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1877F2)
                    )

                    // Text Post Toggle
                    SettingToggleRow(
                        title = "Text Posts",
                        subtitle = "Allow publishing text-only status updates",
                        icon = Icons.Default.TextFields,
                        iconTint = Color(0xFF5E35B1),
                        checked = textPostEnabled,
                        onCheckedChange = {
                            textPostEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Image Post Toggle
                    SettingToggleRow(
                        title = "Image Posts",
                        subtitle = "Allow uploading photos and multi-image posts",
                        icon = Icons.Default.Image,
                        iconTint = Color(0xFF00897B),
                        checked = imagePostEnabled,
                        onCheckedChange = {
                            imagePostEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Video Post Toggle
                    SettingToggleRow(
                        title = "Video Posts & Reels",
                        subtitle = "Allow uploading video clips and reels",
                        icon = Icons.Default.Videocam,
                        iconTint = Color(0xFFD81B60),
                        checked = videoPostEnabled,
                        onCheckedChange = {
                            videoPostEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Story Post Toggle
                    SettingToggleRow(
                        title = "24-Hour Stories",
                        subtitle = "Allow sharing temporary status stories",
                        icon = Icons.Default.AutoAwesome,
                        iconTint = Color(0xFFFB8C00),
                        checked = storyPostEnabled,
                        onCheckedChange = {
                            storyPostEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Link Post Toggle
                    SettingToggleRow(
                        title = "Link Posts",
                        subtitle = "Allow sharing posts containing web links and URLs",
                        icon = Icons.Default.Link,
                        iconTint = Color(0xFF0288D1),
                        checked = linkPostEnabled,
                        onCheckedChange = {
                            linkPostEnabled = it
                            saveAllSettings(silent = true)
                        }
                    )

                    Divider(color = dividerColor, thickness = 0.5.dp)

                    // Maximum Video Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Max Video Duration (Minutes)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "Longest video length allowed per upload",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }

                        OutlinedTextField(
                            value = maxVideoMinutes,
                            onValueChange = { maxVideoMinutes = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(90.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // SECTION 3.1: STORY AUTO-DELETE DURATION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setting_story_auto_delete_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE91E63).copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Story Expiry Time",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "Set hours, minutes, and seconds",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Text(
                        text = "Set how long stories remain visible before being automatically and permanently deleted from Firebase Realtime Database and the app.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 17.sp
                    )

                    // 3 Input Fields: Hours, Minutes, Seconds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = storyExpiryHours,
                            onValueChange = {
                                storyExpiryHours = it.filter { ch -> ch.isDigit() }
                                saveStoryExpiry(silent = true)
                            },
                            label = { Text("Hours (Hr)", fontSize = 11.sp) },
                            placeholder = { Text("24") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("story_expiry_hours_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = storyExpiryMinutes,
                            onValueChange = {
                                storyExpiryMinutes = it.filter { ch -> ch.isDigit() }
                                saveStoryExpiry(silent = true)
                            },
                            label = { Text("Minutes (Min)", fontSize = 11.sp) },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("story_expiry_minutes_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = storyExpirySeconds,
                            onValueChange = {
                                storyExpirySeconds = it.filter { ch -> ch.isDigit() }
                                saveStoryExpiry(silent = true)
                            },
                            label = { Text("Seconds (Sec)", fontSize = 11.sp) },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("story_expiry_seconds_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    // Calculation Banner
                    val h = storyExpiryHours.toIntOrNull() ?: 0
                    val m = storyExpiryMinutes.toIntOrNull() ?: 0
                    val s = storyExpirySeconds.toIntOrNull() ?: 0
                    val totalSec = (h * 3600L) + (m * 60L) + s.toLong()

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkMode) Color(0xFF2C2230) else Color(0xFFFCE4EC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = Color(0xFFD81B60),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Effective Duration: $h hr $m min $s sec",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD81B60)
                                )
                                Text(
                                    text = "Stories will auto-delete $totalSec seconds after posting.",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    // Quick Presets
                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = (h == 0 && m == 0 && s == 30),
                            onClick = {
                                storyExpiryHours = "0"
                                storyExpiryMinutes = "0"
                                storyExpirySeconds = "30"
                                saveStoryExpiry(silent = true)
                            },
                            label = { Text("30s (Test)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = (h == 0 && m == 5 && s == 0),
                            onClick = {
                                storyExpiryHours = "0"
                                storyExpiryMinutes = "5"
                                storyExpirySeconds = "0"
                                saveStoryExpiry(silent = true)
                            },
                            label = { Text("5 mins", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = (h == 1 && m == 0 && s == 0),
                            onClick = {
                                storyExpiryHours = "1"
                                storyExpiryMinutes = "0"
                                storyExpirySeconds = "0"
                                saveStoryExpiry(silent = true)
                            },
                            label = { Text("1 hour", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = (h == 6 && m == 0 && s == 0),
                            onClick = {
                                storyExpiryHours = "6"
                                storyExpiryMinutes = "0"
                                storyExpirySeconds = "0"
                                saveStoryExpiry(silent = true)
                            },
                            label = { Text("6 hours", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = (h == 12 && m == 0 && s == 0),
                            onClick = {
                                storyExpiryHours = "12"
                                storyExpiryMinutes = "0"
                                storyExpirySeconds = "0"
                                saveStoryExpiry(silent = true)
                            },
                            label = { Text("12 hours", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = (h == 24 && m == 0 && s == 0),
                            onClick = {
                                storyExpiryHours = "24"
                                storyExpiryMinutes = "0"
                                storyExpirySeconds = "0"
                                saveStoryExpiry(silent = true)
                            },
                            label = { Text("24 hours (Default)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { saveStoryExpiry(silent = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("save_story_expiry_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Story Expiry Duration",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // SECTION 3.2: HOME FEED CUSTOMIZATION (Admin Only)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setting_home_feed_customization_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1877F2).copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = null,
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Home Feed Control",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "Controlled exclusively by Admin",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Text(
                        text = "Control the ordering of sections (Stories, Images, Videos, Friend Suggestions) and the number of posts per section (2 to 5 items) shown to users on the home feed.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 17.sp
                    )

                    if (onFeedCustomizationClick != null) {
                        Button(
                            onClick = onFeedCustomizationClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("open_feed_customization_from_settings_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Customize Home Feed",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // SECTION 4: DEFAULT DAILY POST LIMITS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Default Daily Post Limits (Global Defaults)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1877F2)
                    )
                    Text(
                        text = "• Set to 0 to completely block/disallow posting that media type.\n• Set to -1 for unlimited posts.\n• Or enter exact daily quota (e.g. 5, 10) that users/pages can post per day.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "User Daily Limits",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFF008937)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = userTextLimit,
                            onValueChange = {
                                userTextLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Text", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = userImageLimit,
                            onValueChange = {
                                userImageLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Image", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = userVideoLimit,
                            onValueChange = {
                                userVideoLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Video", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = userStoryLimit,
                            onValueChange = {
                                userStoryLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Story", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = userLinkLimit,
                            onValueChange = {
                                userLinkLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Link", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Page Daily Limits",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFFE91E63)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = pageTextLimit,
                            onValueChange = {
                                pageTextLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Text", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = pageImageLimit,
                            onValueChange = {
                                pageImageLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Image", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = pageVideoLimit,
                            onValueChange = {
                                pageVideoLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Video", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = pageStoryLimit,
                            onValueChange = {
                                pageStoryLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Story", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = pageLinkLimit,
                            onValueChange = {
                                pageLinkLimit = it
                                if (it.toIntOrNull() != null) saveAllSettings(silent = true)
                            },
                            label = { Text("Link", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // SECTION 5: MONETIZATION REVENUE RATES
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Revenue Rates (per 1000 views)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1877F2)
                    )

                    OutlinedTextField(
                        value = reelRate,
                        onValueChange = { reelRate = it },
                        label = { Text("Reels Video Rate ($ / 1000 views)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = imageRate,
                        onValueChange = { imageRate = it },
                        label = { Text("Image Post Rate ($ / 1000 views)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = textRate,
                        onValueChange = { textRate = it },
                        label = { Text("Text Post Rate ($ / 1000 views)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // SECTION 6: CREATOR FUND ELIGIBILITY & WALLET
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Creator Fund Eligibility Criteria",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1877F2)
                    )

                    OutlinedTextField(
                        value = reqViews,
                        onValueChange = { reqViews = it },
                        label = { Text("Required Reach / Views") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = reqFollowers,
                        onValueChange = { reqFollowers = it },
                        label = { Text("Required Followers") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = reqPosts,
                        onValueChange = { reqPosts = it },
                        label = { Text("Required Total Posts") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = reqReels,
                        onValueChange = { reqReels = it },
                        label = { Text("Required Reels") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = reqAge,
                        onValueChange = { reqAge = it },
                        label = { Text("Required Account Age (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = minTransfer,
                        onValueChange = { minTransfer = it },
                        label = { Text("Minimum Transfer Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // SECTION 7: LEADERBOARD SETTINGS
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
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
                                text = "Leaderboard Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1877F2)
                            )
                            Text(
                                text = "Control total profiles & pages displayed on the leaderboard (Default: 20)",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Divider(color = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB))

                    Text(
                        text = "Total Leaderboard Display Count",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = textPrimary
                    )

                    OutlinedTextField(
                        value = leaderboardLimit,
                        onValueChange = { leaderboardLimit = it },
                        label = { Text("Leaderboard Limit (Default 20)") },
                        placeholder = { Text("e.g., 20") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Quick Selection Chips
                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 20, 30, 50, 100).forEach { preset ->
                            val isSelected = leaderboardLimit == preset.toString()
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF1877F2) else (if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)),
                                modifier = Modifier.clickable { leaderboardLimit = preset.toString() }
                            ) {
                                Text(
                                    text = "$preset",
                                    color = if (isSelected) Color.White else textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "ℹ️ Top 3 entries are displayed in the Gold, Silver, and Bronze podium. The remaining ${(leaderboardLimit.toIntOrNull() ?: 20) - 3} entries are shown in the list below.",
                        fontSize = 12.sp,
                        color = Color(0xFF1877F2),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1877F2)
            )
        )
    }
}
