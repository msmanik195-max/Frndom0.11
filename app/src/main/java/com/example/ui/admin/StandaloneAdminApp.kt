package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AdminRequestRepository
import com.example.ui.menu.admin.AdminDashboardView
import com.example.ui.theme.LocalIsDarkMode

/**
 * Dedicated Standalone Admin Application container for package "com.Flikata.admin".
 * Connects directly to the same Firebase Realtime Database (frndom-871ec) and Storage.
 */
@Composable
fun StandaloneAdminApp(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adminRepo = remember { AdminRequestRepository.getInstance(context) }
    var isAuthenticated by remember { mutableStateOf(false) }

    if (isAuthenticated) {
        AdminDashboardView(
            onBack = { isAuthenticated = false },
            onServerSettingsClick = {},
            modifier = modifier
        )
    } else {
        StandaloneAdminLoginScreen(
            onVerifyPin = { pin -> adminRepo.verifyAdminPin(pin) },
            onAuthenticated = { isAuthenticated = true },
            modifier = modifier
        )
    }
}

@Composable
private fun StandaloneAdminLoginScreen(
    onVerifyPin: (String) -> Boolean,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = LocalIsDarkMode.current
    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val inputBg = if (isDarkMode) Color(0xFF3A3B3C) else Color(0xFFF0F2F5)

    var pinText by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val handleLogin = {
        if (pinText.trim().isEmpty()) {
            errorMessage = "Please enter Admin Security PIN"
        } else if (onVerifyPin(pinText.trim())) {
            errorMessage = ""
            onAuthenticated()
        } else {
            errorMessage = "Incorrect Admin PIN. Please try again."
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgScreen),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = bgCard),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1877F2).copy(alpha = 0.12f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Admin Security",
                            tint = Color(0xFF1877F2),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Flikata Admin Console",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Firebase Project: frndom-871ec",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF45BD62),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter your master Admin PIN to access user controls, verification requests, deposits, and system parameters.",
                    fontSize = 13.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 12) {
                            pinText = it
                            errorMessage = ""
                        }
                    },
                    label = { Text("Admin Security PIN") },
                    placeholder = { Text("Enter 4-8 digit PIN") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (errorMessage.isNotEmpty()) MaterialTheme.colorScheme.error else Color(0xFF1877F2)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN",
                                tint = textSecondary
                            )
                        }
                    },
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { handleLogin() }),
                    singleLine = true,
                    isError = errorMessage.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = Color(0xFF1877F2),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = handleLogin,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Unlock Admin Console",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
