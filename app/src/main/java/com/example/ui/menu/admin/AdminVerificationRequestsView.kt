package com.example.ui.menu.admin

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.VerificationRequestItem
import com.example.data.repository.AdminRequestRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.ui.theme.LocalIsDarkMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationRequestsView(
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
    val walletRepo = remember { WalletRepository.getInstance(context) }

    val verificationRequests by adminRepo.verificationRequestsFlow.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var selectedItemForAction by remember { mutableStateOf<VerificationRequestItem?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "APPROVE" or "REJECT"
    var rejectionReason by remember { mutableStateOf("") }

    // Fullscreen Zoom & Download Image Preview Dialog
    var zoomImageUrl by remember { mutableStateOf<String?>(null) }
    var zoomImageTitle by remember { mutableStateOf("") }

    fun downloadImage(url: String, title: String) {
        try {
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri).apply {
                setTitle("ID Card - $title")
                setDescription("Downloading ID Card image...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "id_card_${System.currentTimeMillis()}.jpg")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(context, "Download started for $title", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredList = remember(verificationRequests, selectedFilter, searchQuery) {
        verificationRequests.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> item.status == "PENDING"
                "APPROVED" -> item.status == "APPROVED"
                "REJECTED" -> item.status == "REJECTED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    item.userName.contains(searchQuery, ignoreCase = true) ||
                    item.userEmail.contains(searchQuery, ignoreCase = true) ||
                    item.userPhone.contains(searchQuery, ignoreCase = true) ||
                    item.planTitle.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val pendingCount = verificationRequests.count { it.status == "PENDING" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgScreen)
            .testTag("admin_verification_requests_screen")
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
                        modifier = Modifier.testTag("verification_requests_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Verification Requests",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            if (pendingCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF00C853)
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
                            text = "Grant Green Verification Badges & process refunds",
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
                    placeholder = { Text("Search by User, Email, Phone or Plan", fontSize = 13.sp, color = textSecondary) },
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
                        focusedBorderColor = Color(0xFF00A86B),
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
                    listOf("ALL" to "All (${verificationRequests.size})", "PENDING" to "Pending ($pendingCount)", "APPROVED" to "Approved", "REJECTED" to "Rejected").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5),
                                labelColor = textPrimary,
                                selectedContainerColor = Color(0xFF00A86B),
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
                            color = if (isDarkMode) Color(0xFF00A86B).copy(alpha = 0.2f) else Color(0xFFE8F5E9),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_verified_badge_green),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No verification requests",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "When users subscribe to Green Verification Badges, requests appear here.",
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
                        VerificationRequestCard(
                            item = item,
                            onApprove = {
                                selectedItemForAction = item
                                actionType = "APPROVE"
                            },
                            onReject = {
                                selectedItemForAction = item
                                actionType = "REJECT"
                                rejectionReason = ""
                            },
                            onViewImage = { url, title ->
                                zoomImageUrl = url
                                zoomImageTitle = title
                            },
                            onDownloadImage = { url, title ->
                                downloadImage(url, title)
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
                    Image(
                        painter = painterResource(id = R.drawable.ic_verified_badge_green),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("Approve Green Badge for ${item.userName}?", fontWeight = FontWeight.Bold, color = textPrimary)
                },
                text = {
                    Column {
                        Text(
                            text = "Confirm approval for ${item.planTitle} (${item.durationDays} days). The Green Verification Badge will be activated immediately on their account.",
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            adminRepo.approveVerificationRequest(item.id, userRepo)
                            selectedItemForAction = null
                            actionType = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                    ) {
                        Text("Grant Green Badge", fontWeight = FontWeight.Bold)
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
                    Text("Reject & Refund BDT ${item.price.toInt()}?", fontWeight = FontWeight.Bold, color = textPrimary)
                },
                text = {
                    Column {
                        Text(
                            text = "Rejecting this verification request will refund BDT ${item.price.toInt()} back to ${item.userName}'s wallet balance. No badge will be granted.",
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            label = { Text("Reason (Optional)", color = textSecondary) },
                            placeholder = { Text("e.g. Account name mismatch with official ID", color = textSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = Color(0xFF00A86B),
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
                            adminRepo.rejectVerificationRequest(item.id, walletRepo, userRepo, rejectionReason)
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

        // Fullscreen Zoom & Pan Preview with Download
        if (zoomImageUrl != null) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            Dialog(
                onDismissRequest = { zoomImageUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = zoomImageTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Pinch to zoom • Drag to pan",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { downloadImage(zoomImageUrl!!, zoomImageTitle) },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                            }

                            IconButton(
                                onClick = { zoomImageUrl = null },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }

                    // Zoomable Image Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 70.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    offset = if (scale > 1f) offset + pan else Offset.Zero
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = zoomImageUrl,
                            contentDescription = zoomImageTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationRequestCard(
    item: VerificationRequestItem,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewImage: (String, String) -> Unit,
    onDownloadImage: (String, String) -> Unit
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
            .testTag("verification_card_${item.id}"),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.userName.ifBlank { "User ${item.userId.take(6)}" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_verified_badge_green),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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

            // Package Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selected Package",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                    Text(
                        text = item.planTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFF00E676) else Color(0xFF008937)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Package Fee",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                    Text(
                        text = "BDT ${item.price.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Duration:", fontSize = 11.sp, color = textSecondary)
                        Text(text = "${item.durationDays} Days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }

                    if (item.userPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Phone:", fontSize = 11.sp, color = textSecondary)
                            Text(text = item.userPhone, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Requested At:", fontSize = 11.sp, color = textSecondary)
                        Text(
                            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.createdAt)),
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

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

            // Attached ID Card Verification (Front & Back)
            if (item.idCardFrontUrl.isNotBlank() || item.idCardBackUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Submitted ID Card Verification",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (item.idCardFrontUrl.isNotBlank()) {
                        IdCardPreviewCard(
                            label = "Front Side",
                            imageUrl = item.idCardFrontUrl,
                            onZoom = { onViewImage(item.idCardFrontUrl, "${item.userName} - ID Front") },
                            onDownload = { onDownloadImage(item.idCardFrontUrl, "${item.userName}_id_front") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (item.idCardBackUrl.isNotBlank()) {
                        IdCardPreviewCard(
                            label = "Back Side",
                            imageUrl = item.idCardBackUrl,
                            onZoom = { onViewImage(item.idCardBackUrl, "${item.userName} - ID Back") },
                            onDownload = { onDownloadImage(item.idCardBackUrl, "${item.userName}_id_back") },
                            modifier = Modifier.weight(1f)
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
                            .testTag("verification_reject_${item.id}"),
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
                            .testTag("verification_approve_${item.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Grant Badge", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdCardPreviewCard(
    label: String,
    imageUrl: String,
    onZoom: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val borderCol = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF0F2F5),
        modifier = modifier.border(1.dp, borderCol, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onZoom() }
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.White else Color(0xFF050505)
                )
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
