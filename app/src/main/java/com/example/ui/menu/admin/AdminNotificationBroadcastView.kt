package com.example.ui.menu.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.NotificationItem
import com.example.data.model.UserProfile
import com.example.data.service.MediaUploadService
import com.example.data.repository.NotificationRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.LocalIsDarkMode
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationBroadcastView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDarkMode = LocalIsDarkMode.current
    val notificationRepo = remember { NotificationRepository(context) }
    val userRepo = remember { UserRepository(context) }
    val uploadService = remember { MediaUploadService.getInstance(context) }

    val allUsers by userRepo.getAllUsersFlow().collectAsState(initial = emptyList())
    val sentNotifications by notificationRepo.getNotificationsFlow("global").collectAsState(initial = emptyList())

    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)

    var targetMode by remember { mutableStateOf("ALL") } // "ALL" or "SPECIFIC"
    var selectedUserId by remember { mutableStateOf("") }
    var selectedUserName by remember { mutableStateOf("") }
    var showUserPicker by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isUploadingImage by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingImage = true
            scope.launch {
                try {
                    val res = uploadService.uploadMedia(uri, "image/jpeg", "notification_banners")
                    val uploaded = res.getOrNull()
                    if (uploaded != null) {
                        imageUrl = uploaded
                        Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "In-App notifications to bell icon",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_notif_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgCard,
                    titleContentColor = textPrimary
                )
            )
        },
        containerColor = bgScreen,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Compose Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = bgCard),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send In-App Notification",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }

                        Text(
                            text = "Recipients will receive this in their top-bar notification center alongside likes and comments.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 16.sp
                        )

                        // Target Selector: All Users vs Specific User
                        Text(
                            text = "Target Audience *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1877F2)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = targetMode == "ALL",
                                onClick = {
                                    targetMode = "ALL"
                                    selectedUserId = ""
                                    selectedUserName = ""
                                },
                                label = { Text("All Users (${allUsers.size})") },
                                leadingIcon = {
                                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1877F2),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )

                            FilterChip(
                                selected = targetMode == "SPECIFIC",
                                onClick = {
                                    targetMode = "SPECIFIC"
                                    showUserPicker = true
                                },
                                label = {
                                    Text(
                                        if (selectedUserName.isNotBlank()) selectedUserName else "Specific User"
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1877F2),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }

                        if (targetMode == "SPECIFIC" && selectedUserId.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE7F3FF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Target: $selectedUserName (ID: $selectedUserId)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1877F2)
                                    )
                                    Text(
                                        text = "Change",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D47A1),
                                        modifier = Modifier.clickable { showUserPicker = true }
                                    )
                                }
                            }
                        }

                        // Title
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Notification Title *") },
                            placeholder = { Text("e.g. System Update, Holiday Notice") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_notif_title_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Description / Content
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Notification Message / Description *") },
                            placeholder = { Text("Write the detailed message here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_notif_desc_input"),
                            shape = RoundedCornerShape(10.dp),
                            minLines = 3
                        )

                        // Optional Image
                        Text(
                            text = "Optional Image (Banner / Visual)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )

                        if (imageUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, dividerColor, RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Uploaded Banner",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { imageUrl = "" },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isUploadingImage
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Uploading Image...")
                                } else {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Attach Image (Optional)")
                                }
                            }
                        }

                        // Direct URL input option
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Or Image URL (Optional)") },
                            placeholder = { Text("https://example.com/image.png") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Send Button
                        Button(
                            onClick = {
                                if (title.isBlank() || description.isBlank()) {
                                    Toast.makeText(context, "Title and description are required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (targetMode == "SPECIFIC" && selectedUserId.isBlank()) {
                                    Toast.makeText(context, "Please select a target user", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSending = true
                                scope.launch {
                                    try {
                                        val now = System.currentTimeMillis()
                                        if (targetMode == "ALL") {
                                            // 1. Post to global notification channel
                                            val globalItem = NotificationItem(
                                                id = "adm_glob_${UUID.randomUUID().toString().take(8)}",
                                                recipientId = "global",
                                                senderId = "admin",
                                                senderName = "Frndom টিম",
                                                senderAvatarUrl = "",
                                                postId = "",
                                                type = "admin_announcement",
                                                title = title.trim(),
                                                content = description.trim(),
                                                imageUrl = imageUrl.trim(),
                                                timestamp = now,
                                                isRead = false
                                            )
                                            notificationRepo.addNotification(globalItem)

                                            // 2. Also send to each active user's personal inbox
                                            allUsers.take(200).forEach { user ->
                                                if (user.uid.isNotBlank()) {
                                                    val personal = globalItem.copy(
                                                        id = "adm_${user.uid.take(4)}_${UUID.randomUUID().toString().take(6)}",
                                                        recipientId = user.uid
                                                    )
                                                    notificationRepo.addNotification(personal)
                                                }
                                            }
                                        } else {
                                            val targetItem = NotificationItem(
                                                id = "adm_${selectedUserId.take(4)}_${UUID.randomUUID().toString().take(6)}",
                                                recipientId = selectedUserId,
                                                senderId = "admin",
                                                senderName = "Frndom টিম",
                                                senderAvatarUrl = "",
                                                postId = "",
                                                type = "admin_announcement",
                                                title = title.trim(),
                                                content = description.trim(),
                                                imageUrl = imageUrl.trim(),
                                                timestamp = now,
                                                isRead = false
                                            )
                                            notificationRepo.addNotification(targetItem)
                                        }

                                        Toast.makeText(context, "Notification sent successfully!", Toast.LENGTH_SHORT).show()
                                        title = ""
                                        description = ""
                                        imageUrl = ""
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSending = false
                                    }
                                }
                            },
                            enabled = !isSending && title.isNotBlank() && description.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_send_notif_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Notification Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // Recent Broadcasts History Header
            item {
                Text(
                    text = "Recent Sent Notifications",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val adminNotifications = sentNotifications.filter { it.type == "admin_announcement" }
            if (adminNotifications.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = textSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No notifications broadcast yet", fontSize = 14.sp, color = textSecondary)
                        }
                    }
                }
            } else {
                items(adminNotifications, key = { it.id }) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgCard),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = Color(0xFF1877F2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = notif.title.ifBlank { "Announcement" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textPrimary
                                    )
                                }

                                val dateStr = remember(notif.timestamp) {
                                    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notif.timestamp))
                                }
                                Text(
                                    text = dateStr,
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = notif.content,
                                fontSize = 13.sp,
                                color = textPrimary,
                                lineHeight = 18.sp
                            )

                            if (notif.imageUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(
                                    model = notif.imageUrl,
                                    contentDescription = "Notification Media",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Target: ${if (notif.recipientId == "global") "All Users" else "User ID: " + notif.recipientId}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF008937)
                                )

                                TextButton(
                                    onClick = {
                                        notificationRepo.removeNotification(notif.recipientId, notif.postId, notif.type, notif.senderId)
                                        Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", color = Color(0xFFE53935), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // User Picker Dialog
    if (showUserPicker) {
        var userSearch by remember { mutableStateOf("") }
        val filteredUsers = remember(userSearch, allUsers) {
            if (userSearch.isBlank()) allUsers else allUsers.filter {
                it.fullName.contains(userSearch, ignoreCase = true) ||
                it.username.contains(userSearch, ignoreCase = true) ||
                it.uid.contains(userSearch, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showUserPicker = false },
            title = { Text("Select User", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    OutlinedTextField(
                        value = userSearch,
                        onValueChange = { userSearch = it },
                        placeholder = { Text("Search by name, username, or ID...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredUsers, key = { it.uid }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedUserId = user.uid
                                        selectedUserName = user.fullName.ifBlank { user.username }
                                        showUserPicker = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE7F3FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (user.fullName.firstOrNull() ?: user.username.firstOrNull() ?: 'U').uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1877F2)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.fullName.ifBlank { user.username },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "@${user.username} • ID: ${user.uid.take(8)}...",
                                        fontSize = 12.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                            Divider(color = dividerColor, thickness = 0.5.dp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}
