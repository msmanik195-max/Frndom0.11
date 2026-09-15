package com.example.ui.friends

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.LocalIsDarkMode

@Composable
fun FriendsScreen(
    currentUserId: String,
    userRepository: UserRepository,
    onUserClick: (UserProfile) -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val allUsers by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())
    val currentUser by userRepository.getUserProfileFlow(currentUserId).collectAsState(initial = null)

    val bgColor = if (isDarkMode) Color(0xFF18191A) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)
    val chipSelectedBg = if (isDarkMode) Color(0xFF263951) else Color(0xFFEBF5FF)
    val chipUnselectedBg = if (isDarkMode) Color(0xFF242526) else Color(0xFFF0F2F5)
    val secondaryBtnBg = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)
    val secondaryBtnText = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)

    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All / Suggestions, 1: Requests, 2: Your Friends
    val dismissedSuggestions = remember { mutableStateListOf<String>() }

    val otherUsers = allUsers.filter { it.uid.isNotBlank() && it.uid != currentUserId }
    val friendRequests = otherUsers.filter { currentUser?.friendRequestsReceivedMap?.get(it.uid) == true }
    val friends = otherUsers.filter { currentUser?.friendsMap?.get(it.uid) == true }
    val suggestions = otherUsers.filter { user ->
        currentUser?.friendsMap?.get(user.uid) != true &&
                currentUser?.friendRequestsReceivedMap?.get(user.uid) != true &&
                !dismissedSuggestions.contains(user.uid)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("friends_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with only Back Button and Friends Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("friends_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Friends",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (friends.isNotEmpty()) {
                    Text(
                        text = "${friends.size} friends",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("Suggestions") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedBg,
                        selectedLabelColor = Color(0xFF0866FF),
                        containerColor = chipUnselectedBg,
                        labelColor = textSecondary
                    )
                )

                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = {
                        Text(
                            text = if (friendRequests.isNotEmpty()) "Requests (${friendRequests.size})" else "Requests"
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (friendRequests.isNotEmpty()) (if (isDarkMode) Color(0xFF5A1E24) else Color(0xFFFFEBEE)) else chipSelectedBg,
                        selectedLabelColor = if (friendRequests.isNotEmpty()) Color(0xFFFA383E) else Color(0xFF0866FF),
                        containerColor = chipUnselectedBg,
                        labelColor = textSecondary
                    )
                )

                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text("Your Friends (${friends.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipSelectedBg,
                        selectedLabelColor = Color(0xFF0866FF),
                        containerColor = chipUnselectedBg,
                        labelColor = textSecondary
                    )
                )
            }

            Divider(thickness = 0.5.dp, color = dividerColor, modifier = Modifier.padding(top = 8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // SECTION 1: Friend Requests
                if (selectedFilter == 1 || (selectedFilter == 0 && friendRequests.isNotEmpty())) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Friend Requests",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${friendRequests.size}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE41E3F)
                                )
                            }
                        }
                    }

                    if (friendRequests.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No pending friend requests",
                                    fontSize = 15.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    } else {
                        items(friendRequests, key = { "req_${it.uid}" }) { requester ->
                            FriendRequestRow(
                                user = requester,
                                isDarkMode = isDarkMode,
                                onConfirm = {
                                    userRepository.acceptFriendRequest(currentUserId, requester.uid)
                                },
                                onDelete = {
                                    userRepository.declineFriendRequest(currentUserId, requester.uid)
                                },
                                onClick = { onUserClick(requester) }
                            )
                        }
                    }

                    if (selectedFilter == 0 && friendRequests.isNotEmpty()) {
                        item {
                            Divider(
                                thickness = 1.dp,
                                color = dividerColor,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // SECTION 2: Suggestions / People You May Know
                if (selectedFilter == 0) {
                    item {
                        Text(
                            text = "People You May Know",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    if (suggestions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No more suggestions available",
                                    fontSize = 14.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    } else {
                        items(suggestions, key = { "sug_${it.uid}" }) { suggestionUser ->
                            val isRequested = currentUser?.friendRequestsSentMap?.get(suggestionUser.uid) == true
                            SuggestionUserRow(
                                user = suggestionUser,
                                isRequested = isRequested,
                                isDarkMode = isDarkMode,
                                onAddFriend = {
                                    userRepository.sendFriendRequest(currentUserId, suggestionUser.uid)
                                },
                                onCancelRequest = {
                                    userRepository.cancelFriendRequest(currentUserId, suggestionUser.uid)
                                },
                                onRemove = {
                                    dismissedSuggestions.add(suggestionUser.uid)
                                },
                                onClick = { onUserClick(suggestionUser) }
                            )
                        }
                    }
                }

                // SECTION 3: Your Friends
                if (selectedFilter == 2) {
                    if (friends.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDarkMode) Color(0xFF263951) else Color(0xFFEBF5FF),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            tint = Color(0xFF0866FF),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Friends Yet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Send friend requests to connect with people on Frndom.",
                                    fontSize = 14.sp,
                                    color = textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(friends, key = { "fr_${it.uid}" }) { friendUser ->
                            FriendUserRow(
                                user = friendUser,
                                isDarkMode = isDarkMode,
                                onUnfriend = {
                                    userRepository.unfriend(currentUserId, friendUser.uid)
                                },
                                onClick = { onUserClick(friendUser) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRequestRow(
    user: UserProfile,
    isDarkMode: Boolean,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val displayName = user.fullName.ifBlank { "${user.firstName} ${user.lastName}".trim() }.ifBlank { "User" }
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val secondaryBtnBg = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)
    val secondaryBtnText = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (user.profilePictureUrl.isNotBlank()) {
                AsyncImage(
                    model = user.profilePictureUrl,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                if (user.isVerificationActive()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerificationBadge(size = 15.dp, show = true)
                }
            }

            if (user.bio.isNotBlank()) {
                Text(
                    text = user.bio,
                    fontSize = 13.sp,
                    color = textSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0866FF)),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text(text = "Confirm", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = secondaryBtnBg,
                        contentColor = secondaryBtnText
                    ),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text(text = "Delete", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SuggestionUserRow(
    user: UserProfile,
    isRequested: Boolean,
    isDarkMode: Boolean,
    onAddFriend: () -> Unit,
    onCancelRequest: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val displayName = user.fullName.ifBlank { "${user.firstName} ${user.lastName}".trim() }.ifBlank { "User" }
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val secondaryBtnBg = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB)
    val secondaryBtnText = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (user.profilePictureUrl.isNotBlank()) {
                AsyncImage(
                    model = user.profilePictureUrl,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                if (user.isVerificationActive()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerificationBadge(size = 15.dp, show = true)
                }
            }

            if (user.bio.isNotBlank()) {
                Text(
                    text = user.bio,
                    fontSize = 13.sp,
                    color = textSecondary,
                    maxLines = 1
                )
            } else if (user.followersCount > 0) {
                Text(
                    text = "${user.followersCount} followers",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isRequested) {
                Button(
                    onClick = onCancelRequest,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = secondaryBtnBg,
                        contentColor = secondaryBtnText
                    ),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text(text = "Cancel Request", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddFriend,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0866FF)),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Friend", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onRemove,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = secondaryBtnBg,
                            contentColor = secondaryBtnText
                        ),
                        modifier = Modifier.weight(0.8f).height(38.dp)
                    ) {
                        Text(text = "Remove", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendUserRow(
    user: UserProfile,
    isDarkMode: Boolean,
    onUnfriend: () -> Unit,
    onClick: () -> Unit
) {
    val displayName = user.fullName.ifBlank { "${user.firstName} ${user.lastName}".trim() }.ifBlank { "User" }
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val menuBg = if (isDarkMode) Color(0xFF242526) else Color.White
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (user.profilePictureUrl.isNotBlank()) {
                AsyncImage(
                    model = user.profilePictureUrl,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                if (user.isVerificationActive()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerificationBadge(size = 15.dp, show = true)
                }
            }

            Text(
                text = "Friends",
                fontSize = 13.sp,
                color = Color(0xFF0866FF),
                fontWeight = FontWeight.Medium
            )
        }

        Box {
            OutlinedButton(
                onClick = { showMenu = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0866FF))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Friends", color = Color(0xFF0866FF))
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(menuBg)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PersonRemove, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unfriend", color = Color.Red)
                        }
                    },
                    onClick = {
                        showMenu = false
                        onUnfriend()
                    }
                )
            }
        }
    }
}
