package com.example.ui.menu.admin

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonetizationRequestItem
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.LocalIsDarkMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMonetizationRequestsView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)
    val inputBg = if (isDarkMode) Color(0xFF3A3B3C) else Color.White

    val context = LocalContext.current
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val userRepo = remember { UserRepository(context) }

    val monetizationRequests by adminRepo.monetizationRequestsFlow.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var selectedItemForAction by remember { mutableStateOf<MonetizationRequestItem?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "APPROVE" or "REJECT"
    var rejectionReason by remember { mutableStateOf("") }

    val filteredList = remember(monetizationRequests, selectedFilter, searchQuery) {
        monetizationRequests.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> item.status == "PENDING"
                "APPROVED" -> item.status == "APPROVED"
                "REJECTED" -> item.status == "REJECTED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    item.userName.contains(searchQuery, ignoreCase = true) ||
                    item.userEmail.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val pendingCount = monetizationRequests.count { it.status == "PENDING" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgScreen)
            .testTag("admin_monetization_requests_screen")
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("monetization_requests_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Monetization Requests",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            if (pendingCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1877F2)
                                ) {
                                    Text(
                                        text = "$pendingCount Pending",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Review Creator Fund applications & partner status",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            }

            // Search and Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Creator Name or Email", fontSize = 13.sp, color = textSecondary) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = textSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = textSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedPlaceholderColor = textSecondary,
                        unfocusedPlaceholderColor = textSecondary,
                        focusedBorderColor = Color(0xFF1877F2),
                        unfocusedBorderColor = dividerColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All (${monetizationRequests.size})", "PENDING" to "Pending ($pendingCount)", "APPROVED" to "Approved", "REJECTED" to "Rejected").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5),
                                labelColor = textPrimary,
                                selectedContainerColor = Color(0xFF1877F2),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Divider(thickness = 0.5.dp, color = dividerColor)

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFE4E6EB),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No monetization requests",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "When creators apply for the Creator Fund, applications appear here.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        MonetizationRequestCard(
                            item = item,
                            onApprove = {
                                selectedItemForAction = item
                                actionType = "APPROVE"
                            },
                            onReject = {
                                selectedItemForAction = item
                                actionType = "REJECT"
                                rejectionReason = ""
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Action confirmation dialogs
        if (selectedItemForAction != null && actionType == "APPROVE") {
            val item = selectedItemForAction!!
            AlertDialog(
                onDismissRequest = {
                    selectedItemForAction = null
                    actionType = null
                },
                containerColor = bgCard,
                titleContentColor = textPrimary,
                textContentColor = textSecondary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("Approve Monetization for ${item.userName}?", fontWeight = FontWeight.Bold, color = textPrimary)
                },
                text = {
                    Column {
                        Text(
                            text = "This creator will be accepted into the Frndom Creator Fund Program. Their dashboard will be updated to Monetized status.",
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            userRepo.setUserMonetized(item.userId, true)
                            adminRepo.approveMonetizationRequest(item.id)
                            selectedItemForAction = null
                            actionType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                    ) {
                        Text("Approve Partner Status", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedItemForAction = null
                        actionType = null
                    }) {
                        Text("Cancel", color = textSecondary)
                    }
                }
            )
        }

        if (selectedItemForAction != null && actionType == "REJECT") {
            val item = selectedItemForAction!!
            AlertDialog(
                onDismissRequest = {
                    selectedItemForAction = null
                    actionType = null
                },
                containerColor = bgCard,
                titleContentColor = textPrimary,
                textContentColor = textSecondary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("Reject Creator Fund Application?", fontWeight = FontWeight.Bold, color = textPrimary)
                },
                text = {
                    Column {
                        Text(
                            text = "Rejecting this application will display a 'Rejected' status on ${item.userName}'s Dashboard.",
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            label = { Text("Reason (Optional)", color = textSecondary) },
                            placeholder = { Text("e.g. Ineligible content or low engagement", color = textSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = Color(0xFF1877F2),
                                unfocusedBorderColor = dividerColor,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            adminRepo.rejectMonetizationRequest(item.id, rejectionReason)
                            selectedItemForAction = null
                            actionType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Reject Application", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedItemForAction = null
                        actionType = null
                    }) {
                        Text("Cancel", color = textSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun MonetizationRequestCard(
    item: MonetizationRequestItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val bgDetails = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF7F8FA)
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFF0F2F5)

    val statusBgColor = when (item.status) {
        "APPROVED" -> if (isDarkMode) Color(0xFF00A86B).copy(alpha = 0.2f) else Color(0xFFE8F8F0)
        "REJECTED" -> if (isDarkMode) Color(0xFFD32F2F).copy(alpha = 0.2f) else Color(0xFFFFEBEE)
        else -> if (isDarkMode) Color(0xFFFFA000).copy(alpha = 0.2f) else Color(0xFFFFF8E1)
    }
    val statusTextColor = when (item.status) {
        "APPROVED" -> if (isDarkMode) Color(0xFF00E676) else Color(0xFF00A86B)
        "REJECTED" -> if (isDarkMode) Color(0xFFFF5252) else Color(0xFFD32F2F)
        else -> if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFE65100)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monetization_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
        elevation = CardDefaults.cardElevation(1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Creator name & Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.userName.ifBlank { "Creator ${item.userId.take(6)}" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    if (item.userEmail.isNotBlank()) {
                        Text(
                            text = item.userEmail,
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBgColor
                ) {
                    Text(
                        text = item.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(thickness = 0.5.dp, color = dividerColor)
            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Summary Grid
            Text(
                text = "Applicant Metrics & Eligibility",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = bgDetails
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(icon = Icons.Default.RemoveRedEye, label = "Views", value = "${item.viewsCount}")
                        MetricPill(icon = Icons.Default.People, label = "Followers", value = "${item.followersCount}")
                        MetricPill(icon = Icons.Default.PostAdd, label = "Posts", value = "${item.postsCount}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(icon = Icons.Default.Movie, label = "Reels", value = "${item.reelsCount}")
                        MetricPill(icon = Icons.Default.DateRange, label = "Account Age", value = "${item.accountAgeDays} days")
                        MetricPill(icon = Icons.Default.AutoGraph, label = "Criteria", value = "5 / 5 Met")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Submitted: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.createdAt))}",
                        fontSize = 11.sp,
                        color = textSecondary
                    )

                    if (item.adminNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Admin Note: ${item.adminNote}",
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Action Buttons for PENDING
            if (item.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("monetization_reject_${item.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("monetization_approve_${item.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve Partner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    icon: ImageVector,
    label: String,
    value: String
) {
    val isDarkMode = LocalIsDarkMode.current
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF1877F2),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = textSecondary)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        }
    }
}
