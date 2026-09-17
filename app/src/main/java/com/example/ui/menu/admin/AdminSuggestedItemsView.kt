package com.example.ui.menu.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.AdminSuggestedItem
import com.example.data.model.GroupItem
import com.example.data.model.PageItem
import com.example.data.model.SuggestedItemType
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.GroupPageRepository
import com.example.data.repository.UserRepository
import com.example.ui.components.VerificationBadge
import com.example.ui.theme.LocalIsDarkMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSuggestedItemsView(
    adminRepo: AdminRequestRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository.getInstance(context) }
    val groupPageRepository = remember { GroupPageRepository(context) }

    val suggestedItems by adminRepo.suggestedItemsFlow.collectAsState()
    val allUsers by userRepository.getAllUsersFlow().collectAsState(initial = emptyList())
    val allPages by groupPageRepository.pagesFlow.collectAsState()
    val allGroups by groupPageRepository.groupsFlow.collectAsState()

    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val inputBg = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5)

    var selectedTab by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All (${suggestedItems.size})", "Profiles", "Pages", "Groups")

    var searchQuery by remember { mutableStateOf("") }
    var itemToEdit by remember { mutableStateOf<AdminSuggestedItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<AdminSuggestedItem?>(null) }

    val filteredItems = remember(suggestedItems, selectedTab, searchQuery) {
        suggestedItems.filter { item ->
            val matchesTab = when (selectedTab) {
                1 -> item.type == SuggestedItemType.PROFILE
                2 -> item.type == SuggestedItemType.PAGE
                3 -> item.type == SuggestedItemType.GROUP
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true) ||
                    item.badgeText.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Suggested Items Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Search screen suggestions (Unlimited)",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("btn_add_suggested_item_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Suggested Item",
                            tint = Color(0xFF1877F2)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgCard
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF1877F2),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_suggested_item")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Suggested Item"
                )
            }
        },
        containerColor = bgScreen
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search suggested items...", fontSize = 14.sp, color = textSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }

            // Category Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = bgCard,
                contentColor = Color(0xFF1877F2),
                edgePadding = 16.dp,
                divider = {}
            ) {
                filterTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color(0xFF1877F2) else textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Information banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Items added here will appear in the Search screen's 'Suggested for you' section. Only admin-approved items are displayed.",
                        fontSize = 12.sp,
                        color = textPrimary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = inputBg,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No items match your search" else "No suggested items yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to suggest profiles, pages, or groups for user search.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Item")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        SuggestedItemCard(
                            item = item,
                            bgCard = bgCard,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onEdit = { itemToEdit = item },
                            onDelete = { itemToDelete = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || itemToEdit != null) {
        SuggestedItemEditDialog(
            initialItem = itemToEdit,
            allUsers = allUsers,
            allPages = allPages,
            allGroups = allGroups,
            onDismiss = {
                showAddDialog = false
                itemToEdit = null
            },
            onSave = { savedItem ->
                if (itemToEdit != null) {
                    adminRepo.updateSuggestedItem(savedItem)
                } else {
                    adminRepo.addSuggestedItem(savedItem)
                }
                showAddDialog = false
                itemToEdit = null
            }
        )
    }

    // Delete confirmation dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Delete Suggested Item",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${target.title}' from the Search screen suggestions?",
                    color = textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminRepo.deleteSuggestedItem(target.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = textPrimary)
                }
            },
            containerColor = bgCard
        )
    }
}

@Composable
private fun SuggestedItemCard(
    item: AdminSuggestedItem,
    bgCard: Color,
    textPrimary: Color,
    textSecondary: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = when (item.type) {
        SuggestedItemType.PROFILE -> Color(0xFF1877F2)
        SuggestedItemType.PAGE -> Color(0xFF2E7D32)
        SuggestedItemType.GROUP -> Color(0xFFE65100)
    }
    val typeBg = typeColor.copy(alpha = 0.12f)
    val typeIcon = when (item.type) {
        SuggestedItemType.PROFILE -> Icons.Default.Person
        SuggestedItemType.PAGE -> Icons.Default.Flag
        SuggestedItemType.GROUP -> Icons.Default.Group
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Image / Avatar
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(if (item.type == SuggestedItemType.GROUP) RoundedCornerShape(12.dp) else CircleShape)
                    .background(typeBg),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Tag
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = typeBg,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = item.type.label,
                            color = typeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (item.badgeText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = item.badgeText,
                                color = Color(0xFFE65100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadge(
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (item.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF1877F2),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestedItemEditDialog(
    initialItem: AdminSuggestedItem?,
    allUsers: List<UserProfile>,
    allPages: List<PageItem>,
    allGroups: List<GroupItem>,
    onDismiss: () -> Unit,
    onSave: (AdminSuggestedItem) -> Unit
) {
    var itemType by remember { mutableStateOf(initialItem?.type ?: SuggestedItemType.PROFILE) }
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var subtitle by remember { mutableStateOf(initialItem?.subtitle ?: "") }
    var targetId by remember { mutableStateOf(initialItem?.targetId ?: "") }
    var imageUrl by remember { mutableStateOf(initialItem?.imageUrl ?: "") }
    var isVerified by remember { mutableStateOf(initialItem?.isVerified ?: false) }
    var badgeText by remember { mutableStateOf(initialItem?.badgeText ?: "") }
    var order by remember { mutableStateOf(initialItem?.order?.toString() ?: "0") }

    var showPicker by remember { mutableStateOf(false) }

    val isDarkMode = LocalIsDarkMode.current
    val bgDialog = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem != null) "Edit Suggested Item" else "Add Suggested Item",
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item Type Selector
                Text("Select Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestedItemType.values().forEach { type ->
                        val isSelected = itemType == type
                        OutlinedButton(
                            onClick = {
                                itemType = type
                                // If changed, optionally clear prefilled data
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) Color(0xFF1877F2).copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (isSelected) Color(0xFF1877F2) else textSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(type.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // Quick Pick Button
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pick Existing ${itemType.label}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Name *") },
                    placeholder = { Text(when (itemType) {
                        SuggestedItemType.PROFILE -> "User Full Name"
                        SuggestedItemType.PAGE -> "Page Name"
                        SuggestedItemType.GROUP -> "Group Name"
                    }) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtitle Input
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle / Description") },
                    placeholder = { Text(when (itemType) {
                        SuggestedItemType.PROFILE -> "e.g. Software Engineer, Dhaka"
                        SuggestedItemType.PAGE -> "e.g. Media/News · 12K Followers"
                        SuggestedItemType.GROUP -> "e.g. Public Group · 5.4K Members"
                    }) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Image URL Input
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image / Avatar URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Badge Text (Optional, e.g. "Featured", "Recommended")
                OutlinedTextField(
                    value = badgeText,
                    onValueChange = { badgeText = it },
                    label = { Text("Custom Badge Tag (Optional)") },
                    placeholder = { Text("e.g. Featured, Top Pick") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority / Order
                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it.filter { char -> char.isDigit() } },
                    label = { Text("Display Order (0 = highest priority)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Verification Badge Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Verification Badge", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textPrimary)
                        Text("Show verified mark in search suggestions", fontSize = 12.sp, color = textSecondary)
                    }
                    Switch(
                        checked = isVerified,
                        onCheckedChange = { isVerified = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1877F2))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedOrder = order.toIntOrNull() ?: 0
                        val finalItem = AdminSuggestedItem(
                            id = initialItem?.id ?: "",
                            type = itemType,
                            targetId = targetId,
                            title = title.trim(),
                            subtitle = subtitle.trim(),
                            imageUrl = imageUrl.trim(),
                            isVerified = isVerified,
                            badgeText = badgeText.trim(),
                            order = parsedOrder,
                            createdAt = initialItem?.createdAt ?: System.currentTimeMillis()
                        )
                        onSave(finalItem)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textPrimary)
            }
        },
        containerColor = bgDialog
    )

    // Picker Dialog for existing users / pages / groups
    if (showPicker) {
        ExistingItemPickerDialog(
            type = itemType,
            allUsers = allUsers,
            allPages = allPages,
            allGroups = allGroups,
            onDismiss = { showPicker = false },
            onSelect = { pickedTitle, pickedSubtitle, pickedTargetId, pickedImageUrl, pickedVerified ->
                title = pickedTitle
                subtitle = pickedSubtitle
                targetId = pickedTargetId
                imageUrl = pickedImageUrl
                isVerified = pickedVerified
                showPicker = false
            }
        )
    }
}

@Composable
private fun ExistingItemPickerDialog(
    type: SuggestedItemType,
    allUsers: List<UserProfile>,
    allPages: List<PageItem>,
    allGroups: List<GroupItem>,
    onDismiss: () -> Unit,
    onSelect: (title: String, subtitle: String, targetId: String, imageUrl: String, isVerified: Boolean) -> Unit
) {
    var pickerSearchQuery by remember { mutableStateOf("") }
    val isDarkMode = LocalIsDarkMode.current
    val bgDialog = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val inputBg = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select an existing ${type.label}",
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                // Search Input inside picker
                OutlinedTextField(
                    value = pickerSearchQuery,
                    onValueChange = { pickerSearchQuery = it },
                    placeholder = { Text("Search ${type.label}...", fontSize = 13.sp, color = textSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (type) {
                        SuggestedItemType.PROFILE -> {
                            val filteredUsers = allUsers.filter {
                                val name = it.fullName.ifBlank { "${it.firstName} ${it.lastName}" }
                                pickerSearchQuery.isBlank() ||
                                        name.contains(pickerSearchQuery, ignoreCase = true) ||
                                        it.email.contains(pickerSearchQuery, ignoreCase = true)
                            }
                            items(filteredUsers, key = { "pick_user_${it.uid}" }) { u ->
                                val name = u.fullName.ifBlank { "${u.firstName} ${u.lastName}" }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelect(
                                                name,
                                                u.bio.ifBlank { u.currentCity.ifBlank { "${u.friendsCount} friends" } },
                                                u.uid,
                                                u.profilePictureUrl,
                                                u.isVerificationActive()
                                            )
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = u.profilePictureUrl,
                                        contentDescription = name,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1877F2).copy(alpha = 0.15f)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textPrimary)
                                        Text(u.email.ifBlank { u.bio }, fontSize = 12.sp, color = textSecondary, maxLines = 1)
                                    }
                                }
                            }
                        }
                        SuggestedItemType.PAGE -> {
                            val filteredPages = allPages.filter {
                                pickerSearchQuery.isBlank() ||
                                        it.name.contains(pickerSearchQuery, ignoreCase = true) ||
                                        it.category.contains(pickerSearchQuery, ignoreCase = true)
                            }
                            items(filteredPages, key = { "pick_page_${it.id}" }) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelect(
                                                p.name,
                                                "${p.category} · ${p.likesCount} followers",
                                                p.id,
                                                p.avatarUrl.ifBlank { p.coverUrl },
                                                p.isVerified
                                            )
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = p.avatarUrl.ifBlank { p.coverUrl },
                                        contentDescription = p.name,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textPrimary)
                                        Text(p.category, fontSize = 12.sp, color = textSecondary, maxLines = 1)
                                    }
                                }
                            }
                        }
                        SuggestedItemType.GROUP -> {
                            val filteredGroups = allGroups.filter {
                                pickerSearchQuery.isBlank() ||
                                        it.name.contains(pickerSearchQuery, ignoreCase = true) ||
                                        it.privacy.contains(pickerSearchQuery, ignoreCase = true)
                            }
                            items(filteredGroups, key = { "pick_grp_${it.id}" }) { g ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelect(
                                                g.name,
                                                "${g.privacy.capitalize()} · ${g.membersCount} members",
                                                g.id,
                                                g.coverUrl,
                                                false
                                            )
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = g.coverUrl,
                                        contentDescription = g.name,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE65100).copy(alpha = 0.15f)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(g.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textPrimary)
                                        Text("${g.privacy.capitalize()} · ${g.membersCount} members", fontSize = 12.sp, color = textSecondary, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = textPrimary)
            }
        },
        containerColor = bgDialog
    )
}
