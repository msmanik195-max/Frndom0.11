package com.example.ui.menu.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.VerificationPlanRepository
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.verification.VerificationPlan
import java.util.UUID

enum class DurationPreset(val label: String, val days: Int, val text: String) {
    DAYS_7("7 Days", 7, "7 Days Validity"),
    DAYS_15("15 Days", 15, "15 Days Validity"),
    MONTH_1("1 Month", 30, "1 Month (30 Days)"),
    MONTHS_3("3 Months", 90, "3 Months (90 Days)"),
    MONTHS_6("6 Months", 180, "6 Months (180 Days)"),
    YEAR_1("1 Year", 365, "1 Year (365 Days)"),
    YEARS_2("2 Years", 730, "2 Years (730 Days)"),
    LIFETIME("Lifetime", 0, "Lifetime Access")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationPackagesView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode = LocalIsDarkMode.current
    val planRepo = remember { VerificationPlanRepository.getInstance(context) }
    val plans by planRepo.plansFlow.collectAsState()

    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<VerificationPlan?>(null) }
    var planToDelete by remember { mutableStateOf<VerificationPlan?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Verification Packages",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "${plans.size} Packages configured",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_packages_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingPlan = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.testTag("add_package_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Package",
                            tint = Color(0xFF00C853)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgCard,
                    titleContentColor = textPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingPlan = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = { Text("Add Package", fontWeight = FontWeight.Bold, color = Color.White) },
                containerColor = Color(0xFF00C853),
                modifier = Modifier.testTag("add_package_fab")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = bgCard),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Green Badge Plans",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Create unlimited packages with custom durations (7 days to lifetime) and prices.",
                                fontSize = 12.sp,
                                color = textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            if (plans.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Packages Created Yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the button below to add your first verification package.",
                                fontSize = 13.sp,
                                color = textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    editingPlan = null
                                    showAddEditDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Package")
                            }
                        }
                    }
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    PackageItemCard(
                        plan = plan,
                        bgCard = bgCard,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        dividerColor = dividerColor,
                        onEdit = {
                            editingPlan = plan
                            showAddEditDialog = true
                        },
                        onDelete = {
                            planToDelete = plan
                        }
                    )
                }
            }

            // Bottom space for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add / Edit Package Dialog
    if (showAddEditDialog) {
        AddEditPackageDialog(
            existingPlan = editingPlan,
            onDismiss = { showAddEditDialog = false },
            onSave = { newOrUpdatedPlan ->
                planRepo.addOrUpdatePlan(newOrUpdatedPlan)
                Toast.makeText(
                    context,
                    if (editingPlan == null) "Package added successfully!" else "Package updated!",
                    Toast.LENGTH_SHORT
                ).show()
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (planToDelete != null) {
        val target = planToDelete!!
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Package?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${target.title}\"? Users will no longer be able to select this package.",
                    fontSize = 14.sp,
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        planRepo.deletePlan(target.id)
                        Toast.makeText(context, "Package deleted", Toast.LENGTH_SHORT).show()
                        planToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PackageItemCard(
    plan: VerificationPlan,
    bgCard: Color,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("package_card_${plan.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = plan.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tag Chip if present
                if (!plan.tag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = plan.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF008937),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Duration and Price Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0F2F5)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF4A4D50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = plan.durationText.ifBlank {
                                if (plan.durationDays <= 0) "Lifetime" else "${plan.durationDays} Days"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1E21)
                        )
                    }
                }

                // Price Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (plan.isFree || plan.price <= 0.0) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                ) {
                    Text(
                        text = if (plan.isFree || plan.price <= 0.0) "FREE" else "${plan.currency} ${if (plan.price % 1.0 == 0.0) plan.price.toInt().toString() else plan.price.toString()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (plan.isFree || plan.price <= 0.0) Color(0xFF008937) else Color(0xFF1565C0),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (plan.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = plan.description,
                    fontSize = 13.sp,
                    color = textSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = dividerColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("edit_package_${plan.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFC62828)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("delete_package_${plan.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPackageDialog(
    existingPlan: VerificationPlan?,
    onDismiss: () -> Unit,
    onSave: (VerificationPlan) -> Unit
) {
    var title by remember { mutableStateOf(existingPlan?.title ?: "") }
    var selectedPreset by remember {
        mutableStateOf(
            DurationPreset.values().firstOrNull { it.days == (existingPlan?.durationDays ?: 30) }
                ?: DurationPreset.MONTH_1
        )
    }
    var customDaysText by remember { mutableStateOf((existingPlan?.durationDays ?: 30).toString()) }
    var isCustomDuration by remember { mutableStateOf(false) }

    var priceText by remember {
        mutableStateOf(
            if (existingPlan != null) {
                if (existingPlan.isFree || existingPlan.price <= 0.0) "0" else existingPlan.price.toString()
            } else "99"
        )
    }
    var selectedCurrency by remember { mutableStateOf(existingPlan?.currency ?: "BDT") }
    var isFree by remember { mutableStateOf(existingPlan?.isFree ?: false) }
    var tag by remember { mutableStateOf(existingPlan?.tag ?: "") }
    var description by remember { mutableStateOf(existingPlan?.description ?: "") }

    val currencyOptions = listOf("BDT", "$", "৳", "₹", "€", "£")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingPlan == null) "Create Verification Package" else "Edit Verification Package",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Package Title *") },
                    placeholder = { Text("e.g. 1 Month Pass, Pro VIP") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Duration Selector Section
                Text(
                    text = "Package Duration *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF008937)
                )

                // Presets FlowRow
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DurationPreset.values().forEach { preset ->
                        val isSelected = !isCustomDuration && selectedPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = preset
                                isCustomDuration = false
                                customDaysText = preset.days.toString()
                            },
                            label = { Text(preset.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00C853),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Price Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Price & Currency *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF008937)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isFree,
                            onCheckedChange = {
                                isFree = it
                                if (it) priceText = "0"
                            }
                        )
                        Text(text = "Free Package", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Currency Picker Chips
                    Column(modifier = Modifier.weight(0.45f)) {
                        Text(text = "Currency", fontSize = 11.sp, color = Color(0xFF65676B))
                        Spacer(modifier = Modifier.height(4.dp))
                        var expandedCurrency by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { expandedCurrency = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Text(text = selectedCurrency, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            currencyOptions.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedCurrency = curr
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }

                    // Price Field
                    OutlinedTextField(
                        value = if (isFree) "0" else priceText,
                        onValueChange = {
                            priceText = it
                            if ((it.toDoubleOrNull() ?: 0.0) > 0.0) {
                                isFree = false
                            }
                        },
                        label = { Text("Amount") },
                        enabled = !isFree,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.55f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Tag / Badge (Optional)
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Badge Tag (Optional)") },
                    placeholder = { Text("e.g. Popular, Save 20%, VIP") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Benefits of this verification plan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        return@Button
                    }
                    val finalDays = if (isCustomDuration) {
                        customDaysText.toIntOrNull() ?: selectedPreset.days
                    } else {
                        selectedPreset.days
                    }
                    val finalDurationText = if (finalDays <= 0) "Lifetime Validity" else "$finalDays Days Validity"
                    val parsedPrice = if (isFree) 0.0 else (priceText.toDoubleOrNull() ?: 0.0)

                    val plan = (existingPlan ?: VerificationPlan(
                        id = "plan_${UUID.randomUUID().toString().take(8)}",
                        title = title.trim(),
                        durationDays = finalDays,
                        durationText = finalDurationText,
                        price = parsedPrice,
                        currency = selectedCurrency,
                        tag = tag.trim().ifBlank { null },
                        description = description.trim(),
                        isFree = isFree || parsedPrice <= 0.0,
                        createdAt = System.currentTimeMillis()
                    )).copy(
                        title = title.trim(),
                        durationDays = finalDays,
                        durationText = finalDurationText,
                        price = parsedPrice,
                        currency = selectedCurrency,
                        tag = tag.trim().ifBlank { null },
                        description = description.trim(),
                        isFree = isFree || parsedPrice <= 0.0
                    )

                    onSave(plan)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                modifier = Modifier.testTag("save_package_btn")
            ) {
                Text("Save Package", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
