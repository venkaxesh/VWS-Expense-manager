package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ExpenseCategory
import com.example.model.PaymentStatus
import com.example.model.Room
import com.example.ui.theme.*
import com.example.viewmodel.AppTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VwsTopAppBar(
    selectedRoom: Room?,
    allRooms: List<Room>,
    onSelectRoom: (String) -> Unit,
    onCreateNewRoom: () -> Unit,
    onJoinRoom: () -> Unit = {},
    onOpenCalendar: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenProfile: () -> Unit,
    isAdmin: Boolean
) {
    var roomDropdownExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color(0xFF111827)
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { roomDropdownExpanded = true }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .testTag("room_selector_btn")
            ) {
                // Official VWS Logo icon from Screenshot 1
                Image(
                    painter = painterResource(id = R.drawable.ic_vws_logo),
                    contentDescription = "VWS Logo",
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VWS",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color(0xFF111827)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Room",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF6B7280)
                        )
                    }
                    Text(
                        text = "Room Expense Manager",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                }

                DropdownMenu(
                    expanded = roomDropdownExpanded,
                    onDismissRequest = { roomDropdownExpanded = false }
                ) {
                    Text(
                        text = "YOUR ROOMS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    allRooms.forEach { room ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(room.name, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                                    Text(room.address, fontSize = 11.sp, color = Color(0xFF6B7280), maxLines = 1)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF047857))
                            },
                            onClick = {
                                onSelectRoom(room.id)
                                roomDropdownExpanded = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("+ Create New Room", color = Color(0xFF047857), fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.AddHome, contentDescription = null, tint = Color(0xFF047857))
                        },
                        onClick = {
                            roomDropdownExpanded = false
                            onCreateNewRoom()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("# Join Room via Code", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color(0xFF2563EB))
                        },
                        onClick = {
                            roomDropdownExpanded = false
                            onJoinRoom()
                        }
                    )
                }
            }
        },
        actions = {
            // Quick Reminder Tone test button
            val context = androidx.compose.ui.platform.LocalContext.current
            IconButton(
                onClick = {
                    com.example.ui.util.ReminderNotificationHelper.playReminderTone(context)
                    android.widget.Toast.makeText(context, "Playing VWS Reminder Tone", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("top_reminder_tone_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Play Reminder Tone",
                    tint = Color(0xFF059669)
                )
            }

            // Calendar icon matching Screenshot 1
            IconButton(
                onClick = onOpenCalendar,
                modifier = Modifier.testTag("top_calendar_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Calendar & Monthly Reports",
                    tint = Color(0xFF111827)
                )
            }

            // About & Version info button
            IconButton(
                onClick = onOpenAbout,
                modifier = Modifier.testTag("top_about_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About VWS & Version",
                    tint = Color(0xFF111827)
                )
            }

            // Admin Dashboard button
            if (isAdmin) {
                IconButton(
                    onClick = onOpenAdmin,
                    modifier = Modifier.testTag("top_admin_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Dashboard",
                        tint = Color(0xFF047857)
                    )
                }
            }

            // User Profile button
            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier.testTag("top_profile_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User Profile",
                    tint = Color(0xFF111827)
                )
            }
        }
    )
}

@Composable
fun VwsBottomNavigation(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    unreadNotesCount: Int
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 4.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.HomeWork,
                    contentDescription = "Home"
                )
            },
            label = {
                Text(
                    "Home",
                    fontWeight = if (currentTab == AppTab.HOME) FontWeight.Bold else FontWeight.Medium,
                    color = if (currentTab == AppTab.HOME) Color(0xFF111827) else Color(0xFF6B7280)
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF059669),
                unselectedIconColor = Color(0xFF6B7280),
                indicatorColor = Color(0xFFECFDF5)
            ),
            modifier = Modifier.testTag("nav_item_home")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.TRANSACTIONS,
            onClick = { onTabSelected(AppTab.TRANSACTIONS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Transaction"
                )
            },
            label = {
                Text(
                    "Transaction",
                    fontWeight = if (currentTab == AppTab.TRANSACTIONS) FontWeight.Bold else FontWeight.Medium,
                    color = if (currentTab == AppTab.TRANSACTIONS) Color(0xFF111827) else Color(0xFF6B7280)
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF059669),
                unselectedIconColor = Color(0xFF6B7280),
                indicatorColor = Color(0xFFECFDF5)
            ),
            modifier = Modifier.testTag("nav_item_transaction")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.NOTES,
            onClick = { onTabSelected(AppTab.NOTES) },
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadNotesCount > 0) {
                            Badge(containerColor = Color(0xFF047857)) {
                                Text("$unreadNotesCount", color = Color.White)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notes"
                    )
                }
            },
            label = {
                Text(
                    "Notes",
                    fontWeight = if (currentTab == AppTab.NOTES) FontWeight.Bold else FontWeight.Medium,
                    color = if (currentTab == AppTab.NOTES) Color(0xFF111827) else Color(0xFF6B7280)
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF059669),
                unselectedIconColor = Color(0xFF6B7280),
                indicatorColor = Color(0xFFECFDF5)
            ),
            modifier = Modifier.testTag("nav_item_notes")
        )
    }
}

@Composable
fun StatusBadge(status: PaymentStatus) {
    val (bgColor, textColor, text) = when (status) {
        PaymentStatus.PAID -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Paid")
        PaymentStatus.PAYMENT_SUBMITTED -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Submitted")
        PaymentStatus.PENDING -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Pending")
        PaymentStatus.DISPUTED -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "Disputed")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CategoryIconBadge(category: ExpenseCategory, size: Int = 40) {
    val (icon, bg, tint) = when (category) {
        ExpenseCategory.ELECTRICITY_EB -> Triple(Icons.Default.Bolt, Color(0xFFF3F4F6), Color(0xFF111827))
        ExpenseCategory.ESSENTIALS -> Triple(Icons.Default.ShoppingCart, Color(0xFFF3F4F6), Color(0xFF111827))
        ExpenseCategory.WATER -> Triple(Icons.Default.WaterDrop, Color(0xFFF3F4F6), Color(0xFF111827))
        ExpenseCategory.ROOM_RENT -> Triple(Icons.Default.Apartment, Color(0xFFF3F4F6), Color(0xFF111827))
        ExpenseCategory.INTERNET -> Triple(Icons.Default.Wifi, Color(0xFFF3F4F6), Color(0xFF111827))
        ExpenseCategory.MAINTENANCE -> Triple(Icons.Default.Build, Color(0xFFF3F4F6), Color(0xFF111827))
        ExpenseCategory.OTHER -> Triple(Icons.Default.Category, Color(0xFFF3F4F6), Color(0xFF111827))
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.displayName,
            tint = tint,
            modifier = Modifier.size((size * 0.55).dp)
        )
    }
}
