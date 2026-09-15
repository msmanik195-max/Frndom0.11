package com.example.ui.menu.admin

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PriceCheck
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WithdrawRequestItem
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.WalletRepository
import com.example.ui.theme.LocalIsDarkMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWithdrawRequestsView(
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
    val walletRepo = remember { WalletRepository.getInstance(context) }
    val clipboardManager = LocalClipboardManager.current

    val withdrawRequests by adminRepo.withdrawRequestsFlow.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var selectedItemForAction by remember { mutableStateOf<WithdrawRequestItem?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "APPROVE" or "REJECT"
    var rejectionReason by remember { mutableStateOf("") }

    val filteredList = remember(withdrawRequests, selectedFilter, searchQuery) {
        withdrawRequests.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> item.status == "PENDING"
                "APPROVED" -> item.status == "APPROVED"
                "REJECTED" -> item.status == "REJECTED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    item.userName.contains(searchQuery, ignoreCase = true) ||
                    item.accountNumber.contains(searchQuery, ignoreCase = true) ||
                    item.methodName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val pendingCount = withdrawRequests.count { it.status == "PENDING" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgScreen)
            .testTag("admin_withdraw_requests_screen")
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
                        modifier = Modifier.testTag("withdraw_requests_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Withdraw Requests",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            if (pendingCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE65100)
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
                            text = "Manage payouts & process balance refunds",
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
                    placeholder = { Text("Search by User, Account or Method", fontSize = 13.sp, color = textSecondary) },
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
                    listOf("ALL" to "All (${withdrawRequests.size})", "PENDING" to "Pending ($pendingCount)", "APPROVED" to "Approved", "REJECTED" to "Rejected").forEach { (key, label) ->
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
                                    imageVector = Icons.Default.PriceCheck,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No withdrawal requests found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "User withdrawal requests will appear here.",
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
                        WithdrawRequestCard(
                            item = item,
                            onCopy = { text, label ->
                                clipboardManager.setText(AnnotatedString(text))
                            },
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
                    Text("Approve Withdrawal BDT ${item.amount.toInt()}?", fontWeight = FontWeight.Bold, color = textPrimary)
                },
                text = {
                    Column {
                        Text(
                            text = "Confirm that you have transferred BDT ${item.amount.toInt()} to ${item.userName} via ${item.methodName} (${item.accountNumber}).",
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Status will be marked as Completed/Approved.",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            adminRepo.approveWithdrawRequest(item.id)
                            selectedItemForAction = null
                            actionType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                    ) {
                        Text("Confirm Payout Sent", fontWeight = FontWeight.Bold)
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
                    Text("Reject & Refund BDT ${item.amount.toInt()}?", fontWeight = FontWeight.Bold, color = textPrimary)
                },
                text = {
                    Column {
                        Text(
                            text = "Rejecting this withdrawal request will immediately refund BDT ${item.amount.toInt()} back to ${item.userName}'s wallet balance.",
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            label = { Text("Reason (Optional)", color = textSecondary) },
                            placeholder = { Text("e.g. Invalid account number or limit exceeded", color = textSecondary) },
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
                            adminRepo.rejectWithdrawRequest(item.id, walletRepo, rejectionReason)
                            selectedItemForAction = null
                            actionType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Reject & Refund Money", fontWeight = FontWeight.Bold)
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
private fun WithdrawRequestCard(
    item: WithdrawRequestItem,
    onCopy: (String, String) -> Unit,
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
            .testTag("withdraw_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
        elevation = CardDefaults.cardElevation(1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: User name & Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.userName.ifBlank { "User ${item.userId.take(6)}" },
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

            // Amount and Method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Withdrawal Amount",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                    Text(
                        text = "BDT ${String.format(Locale.US, "%.2f", item.amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE65100)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE65100).copy(alpha = if (isDarkMode) 0.2f else 0.1f)
                ) {
                    Text(
                        text = item.methodName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFFFF9800) else Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = bgDetails
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Receiver Account Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Send Money To:",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.accountNumber.ifBlank { "N/A" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = textPrimary
                            )
                            if (item.accountNumber.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onCopy(item.accountNumber, "Account number") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Requested At:",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                        Text(
                            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.createdAt)),
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    if (item.adminNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Note: ${item.adminNote}",
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
                            .testTag("withdraw_reject_${item.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject & Refund", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("withdraw_approve_${item.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve Payout", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
