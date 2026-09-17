package com.example.ui.menu.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdPlacementSettings
import com.example.data.repository.AdvertisementRepository
import com.example.ui.theme.LocalIsDarkMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdPlacementSettingsView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode = LocalIsDarkMode.current
    val adRepo = remember { AdvertisementRepository.getInstance(context) }
    val currentSettings by adRepo.placementSettingsFlow.collectAsState()

    var homeEnabled by remember(currentSettings) { mutableStateOf(currentSettings.homeAdsEnabled) }
    var homeIntervalStr by remember(currentSettings) { mutableStateOf(currentSettings.homePostInterval.toString()) }

    var reelsEnabled by remember(currentSettings) { mutableStateOf(currentSettings.reelsAdsEnabled) }
    var reelsIntervalStr by remember(currentSettings) { mutableStateOf(currentSettings.reelsVideoInterval.toString()) }

    var isSaving by remember { mutableStateOf(false) }

    val bgScreen = if (isDarkMode) Color(0xFF18191A) else Color(0xFFF0F2F5)
    val bgCard = if (isDarkMode) Color(0xFF242526) else Color.White
    val textPrimary = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505)
    val textSecondary = if (isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
    val dividerColor = if (isDarkMode) Color(0xFF3E4042) else Color(0xFFE4E6EB)
    val primaryBrand = Color(0xFF1877F2)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("ad_placement_settings_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Advertisement Placement",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Ad Frequency & Feed Injections",
                            fontSize = 12.sp,
                            color = primaryBrand
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ad_placement_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgCard)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgScreen)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro Banner
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1877F2).copy(alpha = 0.15f) else Color(0xFFE7F3FF)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = primaryBrand,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dynamic Ad Placement Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryBrand
                        )
                        Text(
                            text = "Configure how often sponsored posts and video ads appear between organic content on Home Feed and Reels.",
                            fontSize = 12.sp,
                            color = if (isDarkMode) Color(0xFFE4E6EB) else Color(0xFF050505),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // SECTION 1: HOME FEED ADS
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1877F2).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.DynamicFeed, contentDescription = null, tint = primaryBrand, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Home Feed Placement",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Sponsored post cards in main feed",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = homeEnabled,
                            onCheckedChange = { homeEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primaryBrand)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                    Text(
                        text = "Posts Between Sponsored Ads",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = textPrimary
                    )
                    Text(
                        text = "হোম পেজে কতগুলো পোস্টের পর পর স্পনসর্ড অ্যাড প্রদর্শিত হবে",
                        fontSize = 11.sp,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = homeIntervalStr,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 3) {
                                    homeIntervalStr = input
                                }
                            },
                            enabled = homeEnabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_ad_interval_input"),
                            label = { Text("Post Interval (Default: 5)") },
                            trailingIcon = {
                                Text("posts", fontSize = 12.sp, color = textSecondary, modifier = Modifier.padding(end = 12.dp))
                            }
                        )

                        // Quick buttons - / +
                        IconButton(
                            onClick = {
                                val cur = homeIntervalStr.toIntOrNull() ?: 5
                                if (cur > 1) homeIntervalStr = (cur - 1).toString()
                            },
                            enabled = homeEnabled
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = if (homeEnabled) primaryBrand else textSecondary)
                        }

                        IconButton(
                            onClick = {
                                val cur = homeIntervalStr.toIntOrNull() ?: 5
                                if (cur < 50) homeIntervalStr = (cur + 1).toString()
                            },
                            enabled = homeEnabled
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = if (homeEnabled) primaryBrand else textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Example: If set to 5, an ad card appears after every 5 organic posts (5th, 10th, 15th...).",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }

            // SECTION 2: REELS FEED ADS
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE91E63).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reels Feed Placement",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Sponsored video ads in vertical Reels",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = reelsEnabled,
                            onCheckedChange = { reelsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFE91E63))
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                    Text(
                        text = "Videos Between Sponsored Reels",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = textPrimary
                    )
                    Text(
                        text = "রিলসে কতগুলো ভিডিওর পর পর স্পনসর্ড ভিডিও অ্যাড প্রদর্শিত হবে",
                        fontSize = 11.sp,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = reelsIntervalStr,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 3) {
                                    reelsIntervalStr = input
                                }
                            },
                            enabled = reelsEnabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reels_ad_interval_input"),
                            label = { Text("Video Interval (Default: 5)") },
                            trailingIcon = {
                                Text("videos", fontSize = 12.sp, color = textSecondary, modifier = Modifier.padding(end = 12.dp))
                            }
                        )

                        // Quick buttons - / +
                        IconButton(
                            onClick = {
                                val cur = reelsIntervalStr.toIntOrNull() ?: 5
                                if (cur > 1) reelsIntervalStr = (cur - 1).toString()
                            },
                            enabled = reelsEnabled
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = if (reelsEnabled) Color(0xFFE91E63) else textSecondary)
                        }

                        IconButton(
                            onClick = {
                                val cur = reelsIntervalStr.toIntOrNull() ?: 5
                                if (cur < 50) reelsIntervalStr = (cur + 1).toString()
                            },
                            enabled = reelsEnabled
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = if (reelsEnabled) Color(0xFFE91E63) else textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Example: If set to 5, a full-screen sponsored video reel appears after every 5 organic reels.",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Settings Button
            Button(
                onClick = {
                    val homeInt = homeIntervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 5
                    val reelsInt = reelsIntervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 5

                    val updated = AdPlacementSettings(
                        homeAdsEnabled = homeEnabled,
                        homePostInterval = homeInt,
                        reelsAdsEnabled = reelsEnabled,
                        reelsVideoInterval = reelsInt,
                        updatedAt = System.currentTimeMillis()
                    )
                    isSaving = true
                    adRepo.savePlacementSettings(updated)
                    isSaving = false
                    Toast.makeText(context, "Advertisement Placement Settings saved successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_ad_placement_settings_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBrand)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Placement Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
