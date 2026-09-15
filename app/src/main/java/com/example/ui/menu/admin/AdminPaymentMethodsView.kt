package com.example.ui.menu.admin

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.PaymentMethodItem
import com.example.data.repository.AdminRequestRepository
import com.example.ui.theme.LocalIsDarkMode
import java.util.UUID

@Composable
fun AdminPaymentMethodsView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)

    val context = LocalContext.current
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    val paymentMethods by adminRepo.paymentMethodsFlow.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PaymentMethodItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PaymentMethodItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgScreen)
            .testTag("admin_payment_methods_screen")
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
                        modifier = Modifier.testTag("payment_methods_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Payment Methods",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Configure bKash, Nagad, Rocket & Bank accounts",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }

                    Button(
                        onClick = {
                            itemToEdit = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_payment_method_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Method", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Divider(thickness = 0.5.dp, color = dividerColor)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Informational Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF1877F2).copy(alpha = 0.2f) else Color(0xFFE7F3FF)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Account numbers and instructions configured here will appear directly on the user's Deposit and Withdraw screens.",
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFF70B5FF) else Color(0xFF0C3B75),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                items(paymentMethods, key = { it.id }) { method ->
                    PaymentMethodCard(
                        item = method,
                        onToggle = { adminRepo.togglePaymentMethod(method.id) },
                        onEdit = {
                            itemToEdit = method
                            showAddEditDialog = true
                        },
                        onDelete = {
                            itemToDelete = method
                        },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(method.accountNumber))
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditPaymentMethodDialog(
                initialItem = itemToEdit,
                onDismiss = { showAddEditDialog = false },
                onSave = { savedItem ->
                    adminRepo.addOrUpdatePaymentMethod(savedItem)
                    showAddEditDialog = false
                }
            )
        }

        // Delete Confirmation Dialog
        if (itemToDelete != null) {
            val item = itemToDelete!!
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                containerColor = bgCard,
                titleContentColor = textPrimary,
                textContentColor = textSecondary,
                title = { Text("Delete ${item.name} Method?", fontWeight = FontWeight.Bold, color = textPrimary) },
                text = { Text("Are you sure you want to remove this payment method? Users will no longer be able to select it for deposits or withdrawals.", color = textPrimary) },
                confirmButton = {
                    Button(
                        onClick = {
                            adminRepo.deletePaymentMethod(item.id)
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancel", color = textSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun PaymentMethodCard(
    item: PaymentMethodItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val bgDetails = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF7F8FA)
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFF0F2F5)

    val methodColor = try {
        Color(android.graphics.Color.parseColor(item.colorHex))
    } catch (_: Exception) {
        Color(0xFF1877F2)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_method_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgCard),
        elevation = CardDefaults.cardElevation(1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Name pill, Active switch, Action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = methodColor
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.accountType,
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00A86B)
                        ),
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF1877F2), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(thickness = 0.5.dp, color = dividerColor)
            Spacer(modifier = Modifier.height(10.dp))

            // Account Number Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = bgDetails
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Account / Phone Number:", fontSize = 11.sp, color = textSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.accountNumber,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color(0xFF1877F2),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onCopy() }
                            )
                        }
                    }

                    if (item.instructions.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Deposit Instructions for Users:", fontSize = 11.sp, color = textSecondary)
                        Text(
                            text = item.instructions,
                            fontSize = 12.sp,
                            color = textPrimary,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditPaymentMethodDialog(
    initialItem: PaymentMethodItem?,
    onDismiss: () -> Unit,
    onSave: (PaymentMethodItem) -> Unit
) {
    val isDarkMode = LocalIsDarkMode.current
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)
    val inputBg = if (isDarkMode) Color(0xFF3A3B3C) else Color.White

    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var accountNumber by remember { mutableStateOf(initialItem?.accountNumber ?: "") }
    var accountType by remember { mutableStateOf(initialItem?.accountType ?: "Personal (Send Money)") }
    var instructions by remember { mutableStateOf(initialItem?.instructions ?: "") }
    var selectedColor by remember { mutableStateOf(initialItem?.colorHex ?: "#E2136E") }

    val presetColors = listOf(
        "#E2136E" to "bKash Pink",
        "#F7941D" to "Nagad Orange",
        "#8C3494" to "Rocket Purple",
        "#008937" to "Bank Green",
        "#1877F2" to "Primary Blue"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem == null) "Add Payment Method" else "Edit Payment Method",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Method Name (e.g. bKash, Nagad)", color = textSecondary) },
                    singleLine = true,
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

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account / Phone Number", color = textSecondary) },
                    singleLine = true,
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

                OutlinedTextField(
                    value = accountType,
                    onValueChange = { accountType = it },
                    label = { Text("Account Type (e.g. Personal / Merchant)", color = textSecondary) },
                    singleLine = true,
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

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Deposit Instructions for User", color = textSecondary) },
                    minLines = 2,
                    maxLines = 4,
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

                Text(text = "Badge Color Theme:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { (hex, _) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Surface(
                            shape = CircleShape,
                            color = color,
                            border = if (selectedColor == hex) androidx.compose.foundation.BorderStroke(2.dp, if (isDarkMode) Color.White else Color(0xFF050505)) else null,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { selectedColor = hex }
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && accountNumber.isNotBlank()) {
                        val item = initialItem?.copy(
                            name = name.trim(),
                            accountNumber = accountNumber.trim(),
                            accountType = accountType.trim(),
                            instructions = instructions.trim(),
                            colorHex = selectedColor
                        ) ?: PaymentMethodItem(
                            id = "pm_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                            name = name.trim(),
                            accountNumber = accountNumber.trim(),
                            accountType = accountType.trim(),
                            instructions = instructions.trim(),
                            colorHex = selectedColor,
                            isActive = true
                        )
                        onSave(item)
                    }
                },
                enabled = name.isNotBlank() && accountNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
            ) {
                Text("Save Method", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary)
            }
        },
        containerColor = bgCard,
        shape = RoundedCornerShape(16.dp)
    )
}
