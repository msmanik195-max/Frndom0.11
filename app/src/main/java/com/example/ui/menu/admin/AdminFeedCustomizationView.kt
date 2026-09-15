package com.example.ui.menu.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedSectionType
import com.example.data.model.HomeFeedConfig
import com.example.data.model.HomeFeedSection
import com.example.data.repository.AdminRequestRepository
import com.example.ui.theme.LocalIsDarkMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeedCustomizationView(
    adminRepo: AdminRequestRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF393A3B) else Color(0xFFE4E6EB)

    val currentConfig by adminRepo.homeFeedConfigFlow.collectAsState()

    var friendsOnly by remember(currentConfig) { mutableStateOf(currentConfig.onlyFriendsPosts) }
    var sectionsList by remember(currentConfig) {
        mutableStateOf(currentConfig.sections.toMutableList())
    }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showSaveToast by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Home Feed Customization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Order, limits & visibility of home feed blocks",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_feed_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val updated = HomeFeedConfig(
                                onlyFriendsPosts = friendsOnly,
                                sections = sectionsList.toList(),
                                updatedAt = System.currentTimeMillis()
                            )
                            adminRepo.updateHomeFeedConfig(updated)
                            showSaveToast = true
                        },
                        modifier = Modifier.testTag("admin_feed_save_top_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF1877F2))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgCard)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val updated = HomeFeedConfig(
                        onlyFriendsPosts = friendsOnly,
                        sections = sectionsList.toList(),
                        updatedAt = System.currentTimeMillis()
                    )
                    adminRepo.updateHomeFeedConfig(updated)
                    showSaveToast = true
                },
                icon = { Icon(Icons.Default.Save, contentDescription = null, tint = Color.White) },
                text = { Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White) },
                containerColor = Color(0xFF1877F2),
                modifier = Modifier.testTag("admin_feed_save_fab")
            )
        },
        containerColor = bgScreen,
        modifier = modifier.fillMaxSize().testTag("admin_feed_customization_screen")
    ) { innerPadding ->

        LaunchedEffect(showSaveToast) {
            if (showSaveToast) {
                snackbarHostState.showSnackbar("Home Feed settings saved & synced successfully!")
                showSaveToast = false
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. GLOBAL FILTER: Friends Only Posts Toggle Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_friends_only_card"),
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
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (friendsOnly) Color(0xFFE3F2FD) else if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            tint = if (friendsOnly) Color(0xFF1877F2) else textSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Only Friends Posts on Home Feed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (friendsOnly) "Home feed shows only stories, photos & videos from friends" else "Home feed shows posts from everyone across the platform",
                                        fontSize = 12.sp,
                                        color = textSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = friendsOnly,
                                onCheckedChange = { friendsOnly = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1877F2)
                                ),
                                modifier = Modifier.testTag("friends_only_switch")
                            )
                        }

                        if (friendsOnly) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkMode) Color(0xFF1A334E) else Color(0xFFE8F1FC),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF1877F2),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Active: Regular users only see their accepted friends' stories, photos & videos on Home Feed. (Reels screen still displays platform-wide videos).",
                                        fontSize = 11.sp,
                                        color = textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Feed Blocks Order & Range Limits Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Feed Layout & Section Sequence",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Use ▲ ▼ buttons to reposition blocks (e.g., Stories at top or bottom)",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }

                    FilledTonalButton(
                        onClick = { showAddSectionDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_feed_section_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Block", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 3. Dynamic Section Cards with Reorder & Range Controls
            itemsIndexed(
                items = sectionsList,
                key = { _, section -> section.id }
            ) { index, section ->
                FeedSectionConfigCard(
                    section = section,
                    index = index,
                    totalCount = sectionsList.size,
                    isDarkMode = isDarkMode,
                    bgCard = bgCard,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onMoveUp = {
                        if (index > 0) {
                            val updated = sectionsList.toMutableList()
                            val item = updated.removeAt(index)
                            updated.add(index - 1, item)
                            sectionsList = updated
                        }
                    },
                    onMoveDown = {
                        if (index < sectionsList.size - 1) {
                            val updated = sectionsList.toMutableList()
                            val item = updated.removeAt(index)
                            updated.add(index + 1, item)
                            sectionsList = updated
                        }
                    },
                    onToggleEnabled = { enabled ->
                        val updated = sectionsList.toMutableList()
                        updated[index] = section.copy(enabled = enabled)
                        sectionsList = updated
                    },
                    onRangeChange = { min, max ->
                        val updated = sectionsList.toMutableList()
                        updated[index] = section.copy(minCount = min, maxCount = max)
                        sectionsList = updated
                    },
                    onDuplicate = {
                        val newSec = section.copy(
                            id = "sec_${section.type.name.lowercase()}_${System.currentTimeMillis()}"
                        )
                        val updated = sectionsList.toMutableList()
                        updated.add(index + 1, newSec)
                        sectionsList = updated
                    },
                    onDelete = {
                        val updated = sectionsList.toMutableList()
                        updated.removeAt(index)
                        sectionsList = updated
                    }
                )
            }

            // 4. Reset to Default Preset
            item {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        sectionsList = HomeFeedConfig.defaultSections().toMutableList()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("reset_feed_layout_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Facebook Default Preset", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Add Section Dialog
    if (showAddSectionDialog) {
        AddSectionDialog(
            isDarkMode = isDarkMode,
            onDismiss = { showAddSectionDialog = false },
            onAdd = { newType, minVal, maxVal ->
                val newId = "sec_${newType.name.lowercase()}_${System.currentTimeMillis()}"
                val newSec = HomeFeedSection(
                    id = newId,
                    type = newType,
                    title = newType.defaultTitle,
                    enabled = true,
                    minCount = minVal,
                    maxCount = maxVal
                )
                sectionsList = (sectionsList + newSec).toMutableList()
                showAddSectionDialog = false
            }
        )
    }
}

@Composable
fun FeedSectionConfigCard(
    section: HomeFeedSection,
    index: Int,
    totalCount: Int,
    isDarkMode: Boolean,
    bgCard: Color,
    textPrimary: Color,
    textSecondary: Color,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onRangeChange: (Int, Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (section.type) {
        FeedSectionType.STORIES -> Icons.Default.HistoryEdu
        FeedSectionType.IMAGE_POSTS -> Icons.Default.Image
        FeedSectionType.VIDEO_POSTS -> Icons.Default.Videocam
        FeedSectionType.FRIEND_SUGGESTIONS -> Icons.Default.PersonAdd
        FeedSectionType.TEXT_POSTS -> Icons.Default.Article
    }

    val typeColor = when (section.type) {
        FeedSectionType.STORIES -> Color(0xFF1877F2)
        FeedSectionType.IMAGE_POSTS -> Color(0xFF00C853)
        FeedSectionType.VIDEO_POSTS -> Color(0xFFE65100)
        FeedSectionType.FRIEND_SUGGESTIONS -> Color(0xFF7B1FA2)
        FeedSectionType.TEXT_POSTS -> Color(0xFF0097A7)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feed_section_card_${section.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (section.enabled) bgCard else bgCard.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Order Badge, Type Icon, Title, Up/Down controls, Enable Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position badge (#1, #2, etc.)
                    Surface(
                        shape = CircleShape,
                        color = typeColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${index + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = typeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = section.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (section.enabled) textPrimary else textSecondary
                        )
                        Text(
                            text = section.type.name,
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                // Up / Down Reorder Arrows
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp).testTag("move_up_${section.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = if (index > 0) textPrimary else textSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(32.dp).testTag("move_down_${section.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = if (index < totalCount - 1) textPrimary else textSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Switch(
                        checked = section.enabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = typeColor
                        ),
                        modifier = Modifier.testTag("toggle_section_${section.id}")
                    )
                }
            }

            // Controls for range limits (e.g. 2 - 5 items)
            if (section.enabled && section.type != FeedSectionType.STORIES) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Display Batch Range",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Shows between ${section.minCount} to ${section.maxCount} items in this block",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Min Count Selector
                        ItemCountStepper(
                            label = "Min",
                            value = section.minCount,
                            onValueChange = { newMin ->
                                val boundedMin = newMin.coerceIn(1, 20)
                                val boundedMax = maxOf(boundedMin, section.maxCount)
                                onRangeChange(boundedMin, boundedMax)
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text("to", fontSize = 12.sp, color = textSecondary)
                        Spacer(modifier = Modifier.width(8.dp))

                        // Max Count Selector
                        ItemCountStepper(
                            label = "Max",
                            value = section.maxCount,
                            onValueChange = { newMax ->
                                val boundedMax = newMax.coerceIn(1, 30)
                                val boundedMin = minOf(section.minCount, boundedMax)
                                onRangeChange(boundedMin, boundedMax)
                            }
                        )
                    }
                }
            } else if (section.enabled && section.type == FeedSectionType.STORIES) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Stories tray renders horizontally at this exact slot (position #${index + 1}). Pull-to-refresh will automatically reload stories.",
                    fontSize = 11.sp,
                    color = textSecondary,
                    lineHeight = 15.sp
                )
            }

            // Action Buttons: Copy Block & Remove Block
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDuplicate,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1877F2)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("duplicate_section_${section.id}")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Block", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                if (totalCount > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("delete_section_${section.id}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove Block", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCountStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = { if (value > 1) onValueChange(value - 1) },
                modifier = Modifier.size(24.dp)
            ) {
                Text("-", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "$value",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            IconButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.size(24.dp)
            ) {
                Text("+", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddSectionDialog(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onAdd: (FeedSectionType, Int, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(FeedSectionType.IMAGE_POSTS) }
    var minVal by remember { mutableIntStateOf(2) }
    var maxVal by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Feed Section", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select the type of feed block to insert into Home Feed:", fontSize = 13.sp)

                FeedSectionType.entries.forEach { type ->
                    val isSelected = selectedType == type
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF1877F2).copy(alpha = 0.15f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF1877F2) else Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedType = type }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(type.defaultTitle, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (selectedType != FeedSectionType.STORIES) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Batch Range:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ItemCountStepper("Min", minVal) { minVal = it }
                            Text(" to ", fontSize = 12.sp)
                            ItemCountStepper("Max", maxVal) { maxVal = it }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(selectedType, minVal, maxVal) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Text("Add Block")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
