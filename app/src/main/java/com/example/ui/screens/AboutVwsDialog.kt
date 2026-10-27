package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.*

@Composable
fun AboutVwsDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: About & Info, 1: Application License, 2: Cloud Database

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vws_logo),
                            contentDescription = "VWS Logo",
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "About VWS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "Venkaxesh Workspace",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF111827))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs: Info, License, Cloud Database
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF3F4F6),
                    contentColor = Color(0xFF111827)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("App Info", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("tab_about_info")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("License", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("tab_about_license")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Database", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("tab_about_database")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // Info Tab
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Surface(
                                    color = Color(0xFFF9FAFB),
                                    shape = RoundedCornerShape(16.dp),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_vws_logo),
                                            contentDescription = "VWS Logo",
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "VWS — Room Expense Manager",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF111827)
                                        )
                                        Text(
                                            text = "Venkaxesh Workspace Suite",
                                            fontSize = 13.sp,
                                            color = Color(0xFF047857),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Dedicated Developer Profile & Logo Section
                            item {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Surface(
                                    color = Color(0xFF064E3B).copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(18.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth().testTag("card_developer_profile")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp)
                                    ) {
                                        Text(
                                            text = "DEVELOPER PROFILE & CREDENTIALS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Developer Logo
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color(0xFF111827),
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.size(68.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ic_developer_logo),
                                                        contentDescription = "Developer Logo",
                                                        modifier = Modifier.size(54.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Venkaxesh Workspace",
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF111827)
                                                )
                                                Text(
                                                    text = "Lead Architect & Mobile Engineer",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF059669)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "venkaxesh@gmail.com",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF4B5563)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = Color(0xFFE5E7EB))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Tech Stack Badges
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(6.dp)) {
                                                Text("Kotlin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                            Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(6.dp)) {
                                                Text("Jetpack Compose", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(6.dp)) {
                                                Text("Room SQLite", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                            Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(6.dp)) {
                                                Text("Firestore vws-rea", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Reminder Tone & Notification Tone Section
                            item {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.fillMaxWidth().testTag("card_reminder_tone")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF2563EB),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.MusicNote,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = "Reminder Tone (MP3)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E3A8A)
                                                )
                                                Text(
                                                    text = "Custom VWS Notification Sound Active",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF3B82F6)
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                com.example.ui.util.ReminderNotificationHelper.playReminderTone(context)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("btn_play_reminder_tone")
                                        ) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play Tone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "APPLICATION SPECIFICATIONS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                            }

                            item {
                                InfoRow("Application Name", "VWS (Venkaxesh Workspace)")
                                InfoRow("Version", "v1.0.5 (Build 105)")
                                InfoRow("Target Android SDK", "Android 15 / 16 (API 36)")
                                InfoRow("Package ID", "com.aistudio.vwsexpense.kxmpzq")
                                InfoRow("Architecture", "MVVM + Clean Architecture")
                                InfoRow("Financial Engine", "Integer Paise Exact Precision")
                                InfoRow("Reminder Tone", "Custom VWS MP3 Audio Asset")
                                InfoRow("Lead Developer", "Venkaxesh Workspace")
                                InfoRow("Email", "venkaxesh@gmail.com")
                            }

                            item {
                                Text(
                                    text = "ABOUT VENKAXESH WORKSPACE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "VWS (Venkaxesh Workspace) is an enterprise-grade bachelor room and shared apartment financial management platform. It streamlines flat rent, BESCOM electricity bills, Wi-Fi broadbands, water charges, groceries, and maid salaries with zero-loss exact integer paise calculations and instant UPI peer settlements.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF374151),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    1 -> {
                        // License Tab
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Surface(
                                    color = Color(0xFFF9FAFB),
                                    shape = RoundedCornerShape(14.dp),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "VWS Software License Agreement",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF111827)
                                        )
                                        Text(
                                            text = "Copyright © 2026 Venkaxesh Workspace. All rights reserved.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "TERMS OF USE & FINANCIAL PRIVACY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = """
1. Financial Privacy Policy:
All roommate financial records, including bank account numbers, IFSC codes, UPI handles, and payment histories are strictly protected. Bank account numbers are masked by default (XXXX-XXXX-****) and never exposed across unauthorized room members.

2. Audit Trail Invariant:
No financial transactions or adjustment entries may be silently deleted or altered. All corrections must produce an immutable audit log entry documenting the responsible administrator and timestamp.

3. Open Source Components:
This application incorporates Google Jetpack Compose, Android Room Database, Kotlin Coroutines, and Material 3 design libraries under the Apache License 2.0.

4. Limitation of Liability:
VWS acts as an accounting ledger and UPI deep-link dispatch utility. Roommates remain responsible for verifying peer-to-peer bank and UPI payment receipts directly with their financial institutions.
                                    """.trimIndent(),
                                    fontSize = 12.sp,
                                    color = Color(0xFF374151),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    2 -> {
                        // Cloud Database Tab
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(14.dp),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF1D4ED8))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Cloud Firestore Database Ready",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1E3A8A)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Equipped with dual persistence: local high-speed SQLite Room engine + Cloud Firestore schema for cross-device synchronization and Google Authentication.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF3B82F6)
                                        )
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "FIRESTORE COLLECTIONS SCHEMA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                            }

                            item {
                                InfoRow("users", "User profile and encrypted credentials")
                                InfoRow("rooms", "Bachelor room configurations and admin UIDs")
                                InfoRow("roomMembers", "Membership approvals and room roles")
                                InfoRow("expenses", "Original expense bills with integer paise")
                                InfoRow("expenseSplits", "Individual roommate allocations")
                                InfoRow("payments", "UPI & cash payment records and confirmations")
                                InfoRow("roomNotes", "Shared notices, due dates and reminders")
                                InfoRow("auditLogs", "Immutable financial adjustment audit logs")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_about_btn")
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF6B7280))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
    }
}
