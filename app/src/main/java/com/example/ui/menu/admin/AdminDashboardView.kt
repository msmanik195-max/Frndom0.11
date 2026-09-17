package com.example.ui.menu.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.AdvertisementRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GroupPageRepository
import com.example.data.repository.PostRepository
import com.example.data.repository.StoryRepository
import com.example.data.repository.UserRepository
import com.example.ui.maintenance.MaintenanceConfigDialog
import com.example.ui.theme.LocalIsDarkMode
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

enum class AdminActiveScreen {
    DASHBOARD_MAIN,
    USER_MANAGEMENT,
    GROUP_MANAGEMENT,
    PAGE_MANAGEMENT,
    DEPOSIT_REQUESTS,
    WITHDRAW_REQUESTS,
    MONETIZATION_REQUESTS,
    VERIFICATION_REQUESTS,
    VERIFICATION_PACKAGES,
    PAYMENT_METHODS,
    FEED_CUSTOMIZATION,
    SUGGESTED_ITEMS,
    SETTINGS,
    AD_MANAGEMENT,
    ADMIN_NOTIFICATIONS,
    REPORTED_POSTS,
    FILE_MANAGER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardView(
    onBack: () -> Unit,
    onServerSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository(context) }
    val postRepository = remember { PostRepository(context) }
    val storyRepository = remember { StoryRepository(context) }
    val chatRepository = remember { ChatRepository(context) }
    val groupPageRepository = remember { GroupPageRepository(context) }
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val adRepo = remember { AdvertisementRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    val reports by postRepository.reportsFlow.collectAsState()
    val pendingReportsCount = remember(reports) { reports.count { it.status.equals("pending", ignoreCase = true) } }

    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)
    val drawerItemColors = NavigationDrawerItemDefaults.colors(
        unselectedTextColor = textPrimary,
        selectedTextColor = Color(0xFF1877F2),
        selectedContainerColor = if (isDarkMode) Color(0xFF1877F2).copy(alpha = 0.2f) else Color(0xFFE7F3FF)
    )

    // Real-time Data Sources
    val users by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())
    val posts by postRepository.postsFlow.collectAsState()
    val totalMessages by chatRepository.getTotalMessagesCountFlow().collectAsState(initial = 0)
    val groups by groupPageRepository.groupsFlow.collectAsState()
    val pages by groupPageRepository.pagesFlow.collectAsState()
    val allAds by adRepo.advertisementsFlow.collectAsState()

    val totalGroupsCount = groups.size
    val totalPagesCount = pages.size

    val depositRequests by adminRepo.depositRequestsFlow.collectAsState()
    val withdrawRequests by adminRepo.withdrawRequestsFlow.collectAsState()
    val monetizationRequests by adminRepo.monetizationRequestsFlow.collectAsState()
    val verificationRequests by adminRepo.verificationRequestsFlow.collectAsState()
    val paymentMethods by adminRepo.paymentMethodsFlow.collectAsState()

    // Real metrics calculations
    val totalUsersCount = users.size
    val activeUsersCount = users.count { it.isUserOnline() }
    val blockedUsersCount = users.count { it.isBlocked }
    val verifiedUsersCount = users.count { it.isVerificationActive() }
    val monetizedUsersCount = users.count { it.isMonetized }

    val totalPostsCount = posts.size
    val totalCommentsCount = posts.sumOf { it.commentsCount }
    val totalImagePostsCount = posts.count { it.mediaType == "photo" || it.mediaUrls.isNotEmpty() || (it.mediaUrl.isNotBlank() && !it.mediaUrl.endsWith(".mp4")) }
    val totalVideoPostsCount = posts.count { it.mediaType == "video" || it.mediaType == "reel" || it.mediaUrl.endsWith(".mp4") }
    val totalTextPostsCount = posts.count { it.mediaType == "text" && it.mediaUrl.isBlank() && it.mediaUrls.isEmpty() }

    val pendingDepositsCount = depositRequests.count { it.status == "PENDING" }
    val pendingWithdrawsCount = withdrawRequests.count { it.status == "PENDING" }
    val pendingMonetizationsCount = monetizationRequests.count { it.status == "PENDING" }
    val pendingVerificationsCount = verificationRequests.count { it.status == "PENDING" }
    val pendingAdsCount = allAds.count { it.status == "PENDING" }
    val runningAdsCount = allAds.count { it.status == "RUNNING" }

    val totalPendingDepositAmount = depositRequests.filter { it.status == "PENDING" }.sumOf { it.amount }
    val totalPendingWithdrawAmount = withdrawRequests.filter { it.status == "PENDING" }.sumOf { it.amount }

    // Drawer state & Navigation within Admin module
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentAdminScreen by remember { mutableStateOf(AdminActiveScreen.DASHBOARD_MAIN) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    val maintenanceConfig by adminRepo.maintenanceConfigFlow.collectAsState()
    var showMaintenanceConfigDialog by remember { mutableStateOf(false) }

    if (showMaintenanceConfigDialog) {
        MaintenanceConfigDialog(
            initialConfig = maintenanceConfig,
            onSave = { title, desc ->
                adminRepo.setMaintenanceMode(
                    enabled = true,
                    title = title,
                    description = desc
                )
                showMaintenanceConfigDialog = false
            },
            onDismiss = { showMaintenanceConfigDialog = false }
        )
    }

    if (showChangePinDialog) {
        AdminChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onVerifyCurrentPin = { pin -> adminRepo.verifyAdminPin(pin) },
            onSaveNewPin = { newPin ->
                adminRepo.updateAdminPin(newPin) {
                    showChangePinDialog = false
                }
            }
        )
    }

    when (currentAdminScreen) {
        AdminActiveScreen.USER_MANAGEMENT -> {
            AdminUserManagementView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.GROUP_MANAGEMENT -> {
            AdminGroupManagementView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.PAGE_MANAGEMENT -> {
            AdminPageManagementView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.DEPOSIT_REQUESTS -> {
            AdminDepositRequestsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.WITHDRAW_REQUESTS -> {
            AdminWithdrawRequestsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.MONETIZATION_REQUESTS -> {
            AdminMonetizationRequestsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.VERIFICATION_REQUESTS -> {
            AdminVerificationRequestsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.VERIFICATION_PACKAGES -> {
            AdminVerificationPackagesView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.ADMIN_NOTIFICATIONS -> {
            AdminNotificationBroadcastView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.PAYMENT_METHODS -> {
            AdminPaymentMethodsView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.FEED_CUSTOMIZATION -> {
            AdminFeedCustomizationView(
                adminRepo = adminRepo,
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.SUGGESTED_ITEMS -> {
            AdminSuggestedItemsView(
                adminRepo = adminRepo,
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.SETTINGS -> {
            AdminSettingsView(
                adminRepo = adminRepo,
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                onFeedCustomizationClick = { currentAdminScreen = AdminActiveScreen.FEED_CUSTOMIZATION }
            )
        }
        AdminActiveScreen.AD_MANAGEMENT -> {
            AdminAdvertisementManagementView(
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.REPORTED_POSTS -> {
            AdminReportedPostsView(
                postRepository = postRepository,
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.FILE_MANAGER -> {
            AdminFileManagerView(
                postRepository = postRepository,
                storyRepository = storyRepository,
                onBack = { currentAdminScreen = AdminActiveScreen.DASHBOARD_MAIN },
                modifier = modifier
            )
        }
        AdminActiveScreen.DASHBOARD_MAIN -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(310.dp),
                        drawerContainerColor = bgCard
                    ) {
                        // Admin Drawer Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1877F2), Color(0xFF0056B3))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "Admin",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Admin Control Menu",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Platform Management System",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // 1. Dashboard Main
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null, tint = Color(0xFF1877F2)) },
                            label = { Text("Dashboard Overview", fontWeight = FontWeight.SemiBold) },
                            selected = true,
                            onClick = {
                                scope.launch { drawerState.close() }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 2. User Management
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF008937)) },
                            label = { Text("User Management", fontWeight = FontWeight.Medium) },
                            badge = {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkMode) Color(0xFF008937).copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "$totalUsersCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF008937),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 2.1 Group Management
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF1877F2)) },
                            label = { Text("Group Management", fontWeight = FontWeight.Medium) },
                            badge = {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkMode) Color(0xFF1877F2).copy(alpha = 0.2f) else Color(0xFFE7F3FF)
                                ) {
                                    Text(
                                        text = "$totalGroupsCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1877F2),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.GROUP_MANAGEMENT
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 2.2 Page Management
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFE91E63)) },
                            label = { Text("Page Management", fontWeight = FontWeight.Medium) },
                            badge = {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkMode) Color(0xFFE91E63).copy(alpha = 0.2f) else Color(0xFFFCE4EC)
                                ) {
                                    Text(
                                        text = "$totalPagesCount",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE91E63),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.PAGE_MANAGEMENT
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = dividerColor)

                        // 3. Deposit Requests
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF1976D2)) },
                            label = { Text("Deposit Requests", fontWeight = FontWeight.Medium) },
                            badge = {
                                if (pendingDepositsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = "$pendingDepositsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.DEPOSIT_REQUESTS
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 4. Withdraw Requests
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFFE65100)) },
                            label = { Text("Withdraw Requests", fontWeight = FontWeight.Medium) },
                            badge = {
                                if (pendingWithdrawsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = "$pendingWithdrawsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.WITHDRAW_REQUESTS
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 5. Verification Requests
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF00C853)) },
                            label = { Text("Verification Requests", fontWeight = FontWeight.Medium) },
                            badge = {
                                if (pendingVerificationsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = "$pendingVerificationsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.VERIFICATION_REQUESTS
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 5.1 Verification Packages (User request: dynamic packages admin management)
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF00C853)) },
                            label = { Text("Verification Packages", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.VERIFICATION_PACKAGES
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 6. Monetization Requests
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFA000)) },
                            label = { Text("Monetization Requests", fontWeight = FontWeight.Medium) },
                            badge = {
                                if (pendingMonetizationsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = "$pendingMonetizationsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.MONETIZATION_REQUESTS
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 6. Content & Feed Moderation
                        // 6.1 Reported Posts (Content moderation & user report resolution)
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Report, contentDescription = null, tint = Color(0xFFDC2626)) },
                            label = { Text("Reported Posts", fontWeight = FontWeight.Bold) },
                            badge = {
                                if (pendingReportsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFDC2626)
                                    ) {
                                        Text(
                                            text = "$pendingReportsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = currentAdminScreen == AdminActiveScreen.REPORTED_POSTS,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.REPORTED_POSTS
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 6.2 File Manager (Images, Videos, Stories management with database delete)
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF7C3AED)) },
                            label = { Text("File Manager", fontWeight = FontWeight.Bold) },
                            badge = {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkMode) Color(0xFF7C3AED).copy(alpha = 0.2f) else Color(0xFFEDE9FE)
                                ) {
                                    Text(
                                        text = "${posts.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            selected = currentAdminScreen == AdminActiveScreen.FILE_MANAGER,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.FILE_MANAGER
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 6.3 Feed Customization (Admin control for Home feed blocks & friends-only)
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null, tint = Color(0xFF1877F2)) },
                            label = { Text("Feed Customization", fontWeight = FontWeight.Bold) },
                            selected = currentAdminScreen == AdminActiveScreen.FEED_CUSTOMIZATION,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.FEED_CUSTOMIZATION
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 6.3 Suggested Items Management (Search screen suggestions)
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF1877F2)) },
                            label = { Text("Suggested Items", fontWeight = FontWeight.Bold) },
                            selected = currentAdminScreen == AdminActiveScreen.SUGGESTED_ITEMS,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.SUGGESTED_ITEMS
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 6.3 Admin In-App Notifications
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF1877F2)) },
                            label = { Text("Broadcast Notifications", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.ADMIN_NOTIFICATIONS
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = dividerColor)

                        // 7. Payment Methods
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF7B1FA2)) },
                            label = { Text("Payment Methods Setup", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.PAYMENT_METHODS
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 8. Server / Storage Settings
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFF0097A7)) },
                            label = { Text("Cloudflare R2 Storage", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onServerSettingsClick()
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 9. Admin Security PIN
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF2E7D32)) },
                            label = { Text("Security PIN Settings", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    showChangePinDialog = true
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 10. Maintenance Mode
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Construction, contentDescription = null, tint = Color(0xFFE65100)) },
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Maintenance Mode", fontWeight = FontWeight.Medium)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (maintenanceConfig.isEnabled) Color(0xFFFFEBEE) else if (isDarkMode) Color(0xFF008937).copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            text = if (maintenanceConfig.isEnabled) "ON" else "OFF",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (maintenanceConfig.isEnabled) Color(0xFFE53935) else if (isDarkMode) Color(0xFF81C784) else Color(0xFF008937),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    showMaintenanceConfigDialog = true
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )

                        // 10. Settings
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF607D8B)) },
                            label = { Text("Settings", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    currentAdminScreen = AdminActiveScreen.SETTINGS
                                }
                            },
                            colors = drawerItemColors,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "Admin Dashboard",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "Live Real-Time Monitoring",
                                        fontSize = 12.sp,
                                        color = Color(0xFF008937),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            navigationIcon = {
                                // 3-line hamburger menu icon on the left
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("admin_hamburger_menu_btn")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            val totalPending = pendingDepositsCount + pendingWithdrawsCount + pendingMonetizationsCount + pendingVerificationsCount
                                            if (totalPending > 0) {
                                                Badge(
                                                    containerColor = Color(0xFFE53935),
                                                    contentColor = Color.White
                                                ) {
                                                    Text("$totalPending")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Admin Menu",
                                            tint = textPrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { currentAdminScreen = AdminActiveScreen.FEED_CUSTOMIZATION },
                                    modifier = Modifier.testTag("admin_topbar_feed_customization_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "Feed Customization",
                                        tint = Color(0xFF1877F2)
                                    )
                                }
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.testTag("admin_dashboard_close_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = textPrimary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = bgCard)
                        )
                    },
                    containerColor = bgScreen,
                    modifier = modifier.testTag("admin_dashboard_screen")
                ) { innerPadding ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick Action Banner: Jump to User Management (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                                    .testTag("admin_user_mgmt_banner"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1877F2)),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.People,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "User Management",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Block, unblock, verify & manage accounts",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Quick Action Banner: Home Feed Customization (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentAdminScreen = AdminActiveScreen.FEED_CUSTOMIZATION }
                                    .testTag("admin_feed_mgmt_top_banner"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEBF5FF)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1877F2)),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1877F2),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Dashboard,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Home Feed Customization",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = textPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF1877F2)
                                                ) {
                                                    Text(
                                                        text = "FEED",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Customize Stories, Images, Video sequence & count (2-5)",
                                                fontSize = 12.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = Color(0xFF1877F2)
                                    )
                                }
                            }
                        }

                        // Maintenance Mode Banner (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (maintenanceConfig.isEnabled) {
                                            // Click while enabled opens config/disable
                                            showMaintenanceConfigDialog = true
                                        } else {
                                            showMaintenanceConfigDialog = true
                                        }
                                    }
                                    .testTag("admin_maintenance_mode_banner"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (maintenanceConfig.isEnabled) Color(0xFFD32F2F) else bgCard
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (maintenanceConfig.isEnabled) Color.White.copy(alpha = 0.25f) else if (isDarkMode) Color(0xFFE65100).copy(alpha = 0.2f) else Color(0xFFFFF3E0),
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Construction,
                                                        contentDescription = "Maintenance",
                                                        tint = if (maintenanceConfig.isEnabled) Color.White else Color(0xFFE65100),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Maintenance Mode",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = if (maintenanceConfig.isEnabled) Color.White else textPrimary
                                                )
                                                Text(
                                                    text = if (maintenanceConfig.isEnabled) "ACTIVE • App locked for users" else "App is online & accessible",
                                                    fontSize = 12.sp,
                                                    color = if (maintenanceConfig.isEnabled) Color.White.copy(alpha = 0.9f) else textSecondary
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = maintenanceConfig.isEnabled,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    showMaintenanceConfigDialog = true
                                                } else {
                                                    adminRepo.setMaintenanceMode(
                                                        enabled = false,
                                                        title = maintenanceConfig.title,
                                                        description = maintenanceConfig.description
                                                    )
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF00C853),
                                                uncheckedThumbColor = Color.White,
                                                uncheckedTrackColor = Color(0xFFB0BEC5)
                                            )
                                        )
                                    }

                                    if (maintenanceConfig.isEnabled) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Notice: ${maintenanceConfig.description}",
                                                    fontSize = 12.sp,
                                                    color = Color.White,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Edit Notice",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Yellow,
                                                    modifier = Modifier.clickable {
                                                        showMaintenanceConfigDialog = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section 1: User & Community Live Stats (Cards)
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "User & Community Metrics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }

                        // 1. Total Users
                        item {
                            AdminMetricCard(
                                title = "Total Users",
                                count = "$totalUsersCount",
                                subtitle = "Registered users",
                                icon = Icons.Default.People,
                                iconBg = Color(0xFFE7F3FF),
                                iconTint = Color(0xFF1877F2),
                                onClick = { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                            )
                        }

                        // 2. Active Users (Online)
                        item {
                            AdminMetricCard(
                                title = "Active Users",
                                count = "$activeUsersCount",
                                subtitle = "Online currently",
                                icon = Icons.Default.TrendingUp,
                                iconBg = Color(0xFFE8F5E9),
                                iconTint = Color(0xFF00C853),
                                onClick = { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                            )
                        }

                        // 3. Blocked Users
                        item {
                            AdminMetricCard(
                                title = "Blocked Users",
                                count = "$blockedUsersCount",
                                subtitle = "Restricted accounts",
                                icon = Icons.Default.Block,
                                iconBg = Color(0xFFFFEBEE),
                                iconTint = Color(0xFFD32F2F),
                                onClick = { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                            )
                        }

                        // 4. Verified Users (Green Badge)
                        item {
                            AdminMetricCard(
                                title = "Verified Users",
                                count = "$verifiedUsersCount",
                                subtitle = "Active Green Badges",
                                icon = Icons.Default.Verified,
                                iconBg = Color(0xFFE8F5E9),
                                iconTint = Color(0xFF008937),
                                onClick = { currentAdminScreen = AdminActiveScreen.USER_MANAGEMENT }
                            )
                        }

                        // 4.1 Groups Metric
                        item {
                            AdminMetricCard(
                                title = "Total Groups",
                                count = "$totalGroupsCount",
                                subtitle = "User communities",
                                icon = Icons.Default.Group,
                                iconBg = Color(0xFFE7F3FF),
                                iconTint = Color(0xFF1877F2),
                                onClick = { currentAdminScreen = AdminActiveScreen.GROUP_MANAGEMENT }
                            )
                        }

                        // 4.2 Pages Metric
                        item {
                            AdminMetricCard(
                                title = "Total Pages",
                                count = "$totalPagesCount",
                                subtitle = "Creator & brand pages",
                                icon = Icons.Default.Flag,
                                iconBg = Color(0xFFFCE4EC),
                                iconTint = Color(0xFFE91E63),
                                onClick = { currentAdminScreen = AdminActiveScreen.PAGE_MANAGEMENT }
                            )
                        }

                        // Section 2: Posts & Social Engagement
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Engagement & Content Metrics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }

                        // 5. Total Posts
                        item {
                            AdminMetricCard(
                                title = "Total Posts",
                                count = "$totalPostsCount",
                                subtitle = "Feed publications",
                                icon = Icons.Default.PostAdd,
                                iconBg = Color(0xFFEDE7F6),
                                iconTint = Color(0xFF673AB7)
                            )
                        }

                        // 6. Total Comments
                        item {
                            AdminMetricCard(
                                title = "Total Comments",
                                count = "$totalCommentsCount",
                                subtitle = "Post discussions",
                                icon = Icons.Default.Comment,
                                iconBg = Color(0xFFFFF3E0),
                                iconTint = Color(0xFFFF9800)
                            )
                        }

                        // 7. Total Messages Exchanged
                        item {
                            AdminMetricCard(
                                title = "Total Messages",
                                count = "$totalMessages",
                                subtitle = "Chats exchanged",
                                icon = Icons.Default.Chat,
                                iconBg = Color(0xFFE0F7FA),
                                iconTint = Color(0xFF00ACC1)
                            )
                        }

                        // 8. Total Image Posts
                        item {
                            AdminMetricCard(
                                title = "Image Posts",
                                count = "$totalImagePostsCount",
                                subtitle = "Photos published",
                                icon = Icons.Default.Image,
                                iconBg = Color(0xFFFCE4EC),
                                iconTint = Color(0xFFE91E63)
                            )
                        }

                        // 9. Total Video Posts
                        item {
                            AdminMetricCard(
                                title = "Video Posts",
                                count = "$totalVideoPostsCount",
                                subtitle = "Videos & Reels",
                                icon = Icons.Default.Videocam,
                                iconBg = Color(0xFFE1F5FE),
                                iconTint = Color(0xFF03A9F4)
                            )
                        }

                        // 10. Monetized Users
                        item {
                            AdminMetricCard(
                                title = "Monetized Users",
                                count = "$monetizedUsersCount",
                                subtitle = "Creator Fund active",
                                icon = Icons.Default.MonetizationOn,
                                iconBg = Color(0xFFFFF8E1),
                                iconTint = Color(0xFFFFA000),
                                onClick = { currentAdminScreen = AdminActiveScreen.MONETIZATION_REQUESTS }
                            )
                        }

                        // Section 3: Financial & Pending Requests
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Requests & Wallet Pipelines",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }

                        // 11. Pending Deposits
                        item {
                            AdminMetricCard(
                                title = "Pending Deposits",
                                count = "$pendingDepositsCount",
                                subtitle = "BDT ${String.format(Locale.US, "%.0f", totalPendingDepositAmount)}",
                                icon = Icons.Default.AccountBalanceWallet,
                                iconBg = Color(0xFFE8F5E9),
                                iconTint = Color(0xFF2E7D32),
                                badgeAlert = pendingDepositsCount > 0,
                                onClick = { currentAdminScreen = AdminActiveScreen.DEPOSIT_REQUESTS }
                            )
                        }

                        // 12. Pending Withdraws
                        item {
                            AdminMetricCard(
                                title = "Pending Withdraws",
                                count = "$pendingWithdrawsCount",
                                subtitle = "BDT ${String.format(Locale.US, "%.0f", totalPendingWithdrawAmount)}",
                                icon = Icons.Default.AccountBalance,
                                iconBg = Color(0xFFFFEBEE),
                                iconTint = Color(0xFFC62828),
                                badgeAlert = pendingWithdrawsCount > 0,
                                onClick = { currentAdminScreen = AdminActiveScreen.WITHDRAW_REQUESTS }
                            )
                        }

                        // 13. Pending Verifications
                        item {
                            AdminMetricCard(
                                title = "Badge Requests",
                                count = "$pendingVerificationsCount",
                                subtitle = "Pending review",
                                icon = Icons.Default.Verified,
                                iconBg = Color(0xFFE8F5E9),
                                iconTint = Color(0xFF008937),
                                badgeAlert = pendingVerificationsCount > 0,
                                onClick = { currentAdminScreen = AdminActiveScreen.VERIFICATION_REQUESTS }
                            )
                        }

                        // 14. Pending Monetization
                        item {
                            AdminMetricCard(
                                title = "Monetization Req",
                                count = "$pendingMonetizationsCount",
                                subtitle = "Pending creator apps",
                                icon = Icons.Default.MonetizationOn,
                                iconBg = Color(0xFFFFF8E1),
                                iconTint = Color(0xFFE65100),
                                badgeAlert = pendingMonetizationsCount > 0,
                                onClick = { currentAdminScreen = AdminActiveScreen.MONETIZATION_REQUESTS }
                            )
                        }

                        // ==========================================
                        // 3 DISTINCT REAL DATA GRAPHS / CHARTS
                        // ==========================================
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Real-Time Performance Graphs",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }

                        // GRAPH 1: User Base & Community Distribution (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            AdminUserDistributionGraph(
                                totalUsers = totalUsersCount,
                                activeUsers = activeUsersCount,
                                verifiedUsers = verifiedUsersCount,
                                blockedUsers = blockedUsersCount,
                                monetizedUsers = monetizedUsersCount
                            )
                        }

                        // GRAPH 2: Content & Media Breakdown (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            AdminContentMediaGraph(
                                imagePosts = totalImagePostsCount,
                                videoPosts = totalVideoPostsCount,
                                textPosts = totalTextPostsCount,
                                comments = totalCommentsCount,
                                messages = totalMessages
                            )
                        }

                        // GRAPH 3: Financial & Pipeline Activity (Span 2)
                        item(span = { GridItemSpan(2) }) {
                            AdminFinancialPipelineGraph(
                                pendingDeposits = pendingDepositsCount,
                                pendingWithdraws = pendingWithdrawsCount,
                                pendingVerifications = pendingVerificationsCount,
                                pendingMonetizations = pendingMonetizationsCount,
                                totalDeposits = depositRequests.size,
                                totalWithdraws = withdrawRequests.size
                            )
                        }

                        // ==========================================
                        // QUICK LINKS TO ALL MENUS
                        // ==========================================
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Quick Navigation",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }

                        val quickLinks = listOf(
                            Triple("Reported Posts", Icons.Default.Report, AdminActiveScreen.REPORTED_POSTS),
                            Triple("File Manager", Icons.Default.Folder, AdminActiveScreen.FILE_MANAGER),
                            Triple("User Management", Icons.Default.People, AdminActiveScreen.USER_MANAGEMENT),
                            Triple("Group Management", Icons.Default.Groups, AdminActiveScreen.GROUP_MANAGEMENT),
                            Triple("Page Management", Icons.Default.Pages, AdminActiveScreen.PAGE_MANAGEMENT),
                            Triple("Verification Packages", Icons.Default.WorkspacePremium, AdminActiveScreen.VERIFICATION_PACKAGES),
                            Triple("Verification Requests", Icons.Default.Verified, AdminActiveScreen.VERIFICATION_REQUESTS),
                            Triple("Deposit Requests", Icons.Default.AccountBalanceWallet, AdminActiveScreen.DEPOSIT_REQUESTS),
                            Triple("Withdraw Requests", Icons.Default.AccountBalance, AdminActiveScreen.WITHDRAW_REQUESTS),
                            Triple("Send Notifications", Icons.Default.NotificationsActive, AdminActiveScreen.ADMIN_NOTIFICATIONS),
                            Triple("Monetization", Icons.Default.MonetizationOn, AdminActiveScreen.MONETIZATION_REQUESTS),
                            Triple("Payment Setup", Icons.Default.Security, AdminActiveScreen.PAYMENT_METHODS),
                            Triple("Feed Customization", Icons.Default.Dashboard, AdminActiveScreen.FEED_CUSTOMIZATION),
                            Triple("Suggested Items", Icons.Default.Star, AdminActiveScreen.SUGGESTED_ITEMS),
                            Triple("Settings", Icons.Default.Settings, AdminActiveScreen.SETTINGS)
                        )

                        quickLinks.forEach { (title, icon, screen) ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { currentAdminScreen = screen },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = bgCard),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(icon, contentDescription = title, tint = Color(0xFF1877F2), modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textPrimary)
                                    }
                                }
                            }
                        }

                        // Prominent Full-Width Feed Customization Card at the bottom of Admin Dashboard
                        item(span = { GridItemSpan(2) }) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentAdminScreen = AdminActiveScreen.FEED_CUSTOMIZATION }
                                    .testTag("admin_feed_customization_bottom_banner"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEBF5FF)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1877F2))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1877F2),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Dashboard,
                                                contentDescription = "Feed Customization",
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Home Feed Customization",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "Customize Stories, Images & Video sequence, counts (2-5), and friends-only toggle",
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFF1877F2)
                                    )
                                }
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    count: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    badgeAlert: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val cardBg = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val actualIconBg = if (isDarkMode) iconTint.copy(alpha = 0.2f) else iconBg

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = actualIconBg,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (badgeAlert) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE53935),
                        modifier = Modifier.size(10.dp)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = count,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// GRAPH 1: USER COMMUNITY DISTRIBUTION
// ==========================================
@Composable
fun AdminUserDistributionGraph(
    totalUsers: Int,
    activeUsers: Int,
    verifiedUsers: Int,
    blockedUsers: Int,
    monetizedUsers: Int
) {
    val isDarkMode = LocalIsDarkMode.current
    val cardBg = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val guideLineColor = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "1. User Community Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Distribution across total $totalUsers registered users",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDarkMode) Color(0xFF1877F2).copy(alpha = 0.2f) else Color(0xFFE7F3FF)
                ) {
                    Text(
                        text = "LIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1877F2),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = maxOf(totalUsers, 1).toFloat()

            // Custom Canvas Chart with rounded bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val barSpacing = size.width / 5f
                val barWidth = barSpacing * 0.5f

                val items = listOf(
                    Triple("Total", totalUsers, Color(0xFF1877F2)),
                    Triple("Active", activeUsers, Color(0xFF00C853)),
                    Triple("Verified", verifiedUsers, Color(0xFF008937)),
                    Triple("Monetized", monetizedUsers, Color(0xFFFFA000)),
                    Triple("Blocked", blockedUsers, Color(0xFFD32F2F))
                )

                // Background horizontal guide lines
                val lines = 3
                for (i in 0..lines) {
                    val y = size.height * (i.toFloat() / lines)
                    drawLine(
                        color = guideLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                items.forEachIndexed { index, (label, value, color) ->
                    val x = index * barSpacing + (barSpacing - barWidth) / 2f
                    val heightRatio = if (maxVal > 0) (value.toFloat() / maxVal).coerceIn(0.08f, 1f) else 0.08f
                    val barHeight = (size.height - 20.dp.toPx()) * heightRatio
                    val y = size.height - barHeight

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend & Values Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatBadgeItem(label = "Total", value = "$totalUsers", color = Color(0xFF1877F2))
                StatBadgeItem(label = "Active", value = "$activeUsers", color = Color(0xFF00C853))
                StatBadgeItem(label = "Verified", value = "$verifiedUsers", color = Color(0xFF008937))
                StatBadgeItem(label = "Monetized", value = "$monetizedUsers", color = Color(0xFFFFA000))
                StatBadgeItem(label = "Blocked", value = "$blockedUsers", color = Color(0xFFD32F2F))
            }
        }
    }
}

// ==========================================
// GRAPH 2: CONTENT & MEDIA ENGAGEMENT GRAPH
// ==========================================
@Composable
fun AdminContentMediaGraph(
    imagePosts: Int,
    videoPosts: Int,
    textPosts: Int,
    comments: Int,
    messages: Int
) {
    val isDarkMode = LocalIsDarkMode.current
    val cardBg = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "2. Media & Social Traffic",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Images, Videos, Texts, Comments & Messages",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDarkMode) Color(0xFF673AB7).copy(alpha = 0.2f) else Color(0xFFEDE7F6)
                ) {
                    Text(
                        text = "REAL-TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxCount = maxOf(imagePosts, videoPosts, textPosts, comments, messages, 1).toFloat()

            val rows = listOf(
                Pair("Photos ($imagePosts)", Triple(imagePosts, Color(0xFFE91E63), if (isDarkMode) Color(0xFFE91E63).copy(alpha = 0.15f) else Color(0xFFFCE4EC))),
                Pair("Videos ($videoPosts)", Triple(videoPosts, Color(0xFF03A9F4), if (isDarkMode) Color(0xFF03A9F4).copy(alpha = 0.15f) else Color(0xFFE1F5FE))),
                Pair("Texts ($textPosts)", Triple(textPosts, Color(0xFF9C27B0), if (isDarkMode) Color(0xFF9C27B0).copy(alpha = 0.15f) else Color(0xFFF3E5F5))),
                Pair("Comments ($comments)", Triple(comments, Color(0xFFFF9800), if (isDarkMode) Color(0xFFFF9800).copy(alpha = 0.15f) else Color(0xFFFFF3E0))),
                Pair("Messages ($messages)", Triple(messages, Color(0xFF00BCD4), if (isDarkMode) Color(0xFF00BCD4).copy(alpha = 0.15f) else Color(0xFFE0F7FA)))
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (label, data) ->
                    val (valCount, color, bg) = data
                    val ratio = (valCount.toFloat() / maxCount).coerceIn(0.04f, 1f)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            Text(text = "$valCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(bg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// GRAPH 3: FINANCIAL & VERIFICATION PIPELINE
// ==========================================
@Composable
fun AdminFinancialPipelineGraph(
    pendingDeposits: Int,
    pendingWithdraws: Int,
    pendingVerifications: Int,
    pendingMonetizations: Int,
    totalDeposits: Int,
    totalWithdraws: Int
) {
    val isDarkMode = LocalIsDarkMode.current
    val cardBg = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "3. Financial & Request Pipelines",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Pending deposits, withdrawals & verification queues",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDarkMode) Color(0xFFE65100).copy(alpha = 0.2f) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = "PIPELINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxRequests = maxOf(pendingDeposits, pendingWithdraws, pendingVerifications, pendingMonetizations, 1).toFloat()

            // Custom multi-column pipeline canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                val colWidth = size.width / 4f
                val barW = colWidth * 0.55f

                val pipelines = listOf(
                    Triple("Deposits", pendingDeposits, Color(0xFF2E7D32)),
                    Triple("Withdraws", pendingWithdraws, Color(0xFFC62828)),
                    Triple("Badge Req", pendingVerifications, Color(0xFF008937)),
                    Triple("Monetize", pendingMonetizations, Color(0xFFFFA000))
                )

                pipelines.forEachIndexed { i, (name, valCount, color) ->
                    val x = i * colWidth + (colWidth - barW) / 2f
                    val heightRatio = if (maxRequests > 0) (valCount.toFloat() / maxRequests).coerceIn(0.1f, 1f) else 0.1f
                    val barH = (size.height - 15.dp.toPx()) * heightRatio
                    val y = size.height - barH

                    // Draw bar
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatBadgeItem(label = "Deposits", value = "$pendingDeposits Pending", color = Color(0xFF2E7D32))
                StatBadgeItem(label = "Withdraws", value = "$pendingWithdraws Pending", color = Color(0xFFC62828))
                StatBadgeItem(label = "Badge", value = "$pendingVerifications Pending", color = Color(0xFF008937))
                StatBadgeItem(label = "Monetize", value = "$pendingMonetizations Pending", color = Color(0xFFFFA000))
            }
        }
    }
}

@Composable
fun StatBadgeItem(
    label: String,
    value: String,
    color: Color
) {
    val isDarkMode = LocalIsDarkMode.current
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(8.dp)
        ) {}
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(text = label, fontSize = 10.sp, color = textSecondary)
    }
}
