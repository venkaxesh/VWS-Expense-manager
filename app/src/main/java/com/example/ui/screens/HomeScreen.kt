package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.*
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.*
import com.example.viewmodel.VwsUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: VwsUiState,
    onAddExpenseClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onExpenseClick: (Expense) -> Unit,
    onCreateRoomClick: () -> Unit = {},
    onJoinRoomClick: () -> Unit = {},
    onDeleteRoomClick: (String) -> Unit = {},
    onAddMemberByCode: (String) -> Unit = {},
    onDirectPayGooglePay: (User, Long, String) -> Unit = { _, _, _ -> },
    onDirectPayCash: (User, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val currentRoom = uiState.currentRoom
    val summary = uiState.financialSummary
    val userMap = remember(uiState.users) { uiState.users.associateBy { it.id } }
    val roomMembers = remember(uiState.members, currentRoom?.id, uiState.users) {
        if (currentRoom == null) emptyList()
        else {
            val memberUserIds = uiState.members.filter { it.roomId == currentRoom.id }.map { it.userId }.toSet()
            uiState.users.filter { memberUserIds.contains(it.id) || it.id == currentRoom.adminUid }
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    var showRoomProfileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var splitFilter by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "SETTLED"

    val displayedExpenses = remember(uiState.expenses, splitFilter, uiState.splits, uiState.currentUserId) {
        when (splitFilter) {
            "PENDING" -> uiState.expenses.filter { exp ->
                val mySplit = uiState.splits.firstOrNull { it.expenseId == exp.id && it.memberUserId == uiState.currentUserId }
                mySplit != null && !mySplit.isSettled
            }
            "SETTLED" -> uiState.expenses.filter { exp ->
                val mySplit = uiState.splits.firstOrNull { it.expenseId == exp.id && it.memberUserId == uiState.currentUserId }
                mySplit != null && mySplit.isSettled
            }
            else -> uiState.expenses
        }
    }

    // Room Profile & Info Dialog
    if (showRoomProfileDialog && currentRoom != null) {
        RoomProfileDialog(
            room = currentRoom,
            members = roomMembers,
            currentUserId = uiState.currentUserId,
            isAdmin = uiState.isCurrentAdmin || currentRoom.adminUid == uiState.currentUserId,
            onDismiss = { showRoomProfileDialog = false },
            onDeleteRoom = {
                showRoomProfileDialog = false
                showDeleteConfirmDialog = true
            },
            onAddMemberByCode = onAddMemberByCode
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && currentRoom != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Room '${currentRoom.name}'?") },
            text = { Text("Are you sure you want to delete this room? This action will remove all shared room data and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRoomClick(currentRoom.id)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_delete_room")
                ) {
                    Text("Delete Room", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP NAVBAR: Logo + VWS Title + Prominent "Room Profile" Header Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_top_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vws_logo),
                            contentDescription = "VWS Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Kaasu Kudu Kaasu",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "Room Expense Splitter",
                                fontSize = 11.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Room Profile Header Button
                    if (currentRoom != null) {
                        Button(
                            onClick = { showRoomProfileDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECFDF5)),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("btn_room_profile_header")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = "Room Profile",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = currentRoom.name.take(14),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "Room Profile & Info",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF047857)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onCreateRoomClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.testTag("btn_create_room_header")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Room", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ONBOARDING IF NO ROOM
        if (currentRoom == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFECFDF5),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("No Active Room Selected", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Create a new bachelor room or join an existing room with a 6-digit code to start splitting expenses.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onCreateRoomClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_create_room_welcome")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Room")
                            }
                            OutlinedButton(
                                onClick = onJoinRoomClick,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF2563EB)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                                modifier = Modifier.testTag("btn_join_room_welcome")
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Join via Code")
                            }
                        }
                    }
                }
            }
        } else {
            // SUMMARY SPLIT CARDS (Clutter-free, modern M3)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Your Share Outstanding
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_your_share"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Your Share", fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = CurrencyHelper.formatPaise(summary.allocatedSharePaise),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total allocated split",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }

                    // You Have Paid
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_you_have_paid"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("You Paid", fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFECFDF5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = CurrencyHelper.formatPaise(summary.totalPaidPaise),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF059669)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Settled so far",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }



            // SPLIT DETAILS SECTION (Clutter-free and focused)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Split Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFECFDF5)
                        ) {
                            Text(
                                text = "${uiState.expenses.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Filter Chips: ALL, PENDING, SETTLED
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("ALL" to "All", "PENDING" to "Pending", "SETTLED" to "Settled").forEach { (key, label) ->
                            val isSelected = splitFilter == key
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF111827) else Color(0xFFF3F4F6),
                                modifier = Modifier.clickable { splitFilter = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF4B5563),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (displayedExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No split expenses found", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap '+ Add Expense' below to add a room bill.", fontSize = 12.sp, color = Color(0xFF6B7280))
                        }
                    }
                }
            } else {
                items(displayedExpenses, key = { it.id }) { expense ->
                    val mySplit = uiState.splits.firstOrNull { it.expenseId == expense.id && it.memberUserId == uiState.currentUserId }
                    val payerUser = userMap[expense.payerUserId]
                    val isPending = mySplit != null && !mySplit.isSettled

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExpenseClick(expense) }
                            .testTag("split_card_${expense.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (isPending) Color(0xFFFDE68A) else Color(0xFFE5E7EB)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CategoryIconBadge(category = expense.category, size = 36)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = expense.description,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111827),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Paid by ${payerUser?.fullName ?: expense.payerUserId} • ${dateFormat.format(Date(expense.expenseDateMillis))}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }

                                // Total Bill vs Split Amount
                                Column(horizontalAlignment = Alignment.End) {
                                    val mySplitPaise = mySplit?.allocatedAmountPaise ?: (expense.totalAmountPaise / roomMembers.size.coerceAtLeast(1))
                                    Text(
                                        text = CurrencyHelper.formatPaise(mySplitPaise),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isPending) Color(0xFFB45309) else Color(0xFF111827)
                                    )
                                    Text(
                                        text = "of ${CurrencyHelper.formatPaise(expense.totalAmountPaise)}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Action Bar: Status Pill + 1-Tap "⚡ Pay UPI" button if pending
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (mySplit != null && mySplit.isSettled) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFECFDF5)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Settled ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pending Share", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1-Tap Pay UPI Button on the split card
                                    if (isPending && expense.payerUserId != uiState.currentUserId) {
                                        Button(
                                            onClick = {
                                                val payee = payerUser ?: User(
                                                    id = expense.payerUserId,
                                                    username = "payer",
                                                    fullName = "Roommate Payer",
                                                    email = "",
                                                    upiId = "vwsroom@upi",
                                                    role = UserRole.MEMBER
                                                )
                                                val splitPaise = mySplit?.allocatedAmountPaise ?: (expense.totalAmountPaise / roomMembers.size.coerceAtLeast(1))
                                                onDirectPayGooglePay(payee, splitPaise, "Split: ${expense.description}")
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                            modifier = Modifier.height(30.dp).testTag("btn_pay_split_upi_${expense.id}")
                                        ) {
                                            Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pay UPI", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // View detail arrow
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Details",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ROOM PROFILE & INFO DIALOG (Opened via Header Button)
// Displays Room Details + Roommates / Members
// ==========================================
@Composable
fun RoomProfileDialog(
    room: Room,
    members: List<User>,
    currentUserId: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onDeleteRoom: () -> Unit,
    onAddMemberByCode: (String) -> Unit
) {
    val context = LocalContext.current
    var addMemberInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Room Profile & Infos",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = room.name,
                                fontSize = 12.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF4B5563))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card 1: 6-Digit Invitation Code & Share Link
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ROOM INVITATION CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = room.invitationCode.ifBlank { "N/A" },
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF065F46),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 1-Click Copy Code
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("VWS Room Code", room.invitationCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Room Code ${room.invitationCode} copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Code", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                    }

                                    // Share Link
                                    Button(
                                        onClick = {
                                            val inviteLink = "vws://join?code=${room.invitationCode}&roomId=${room.id}"
                                            val shareText = "Join my bachelor room '${room.name}' on VWS Expense Manager!\nRoom Code: ${room.invitationCode}\nInvite Link: $inviteLink"
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Room Invitation"))
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share Link", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Card 2: Complete Room Details & Utilities
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Property & Utility Infos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                RoomInfoRow(label = "Monthly Rent", value = "₹${room.monthlyRentAmount / 100} / month", icon = Icons.Default.CurrencyRupee)
                                RoomInfoRow(label = "Rent Due Day", value = "Day ${room.rentDueDay} of every month", icon = Icons.Default.CalendarToday)

                                if (room.ebConsumerNumber.isNotBlank()) {
                                    RoomInfoRow(label = "EB Consumer No", value = room.ebConsumerNumber, icon = Icons.Default.FlashOn)
                                }
                                if (room.waterTaxNumber.isNotBlank()) {
                                    RoomInfoRow(label = "Water Connection No", value = room.waterTaxNumber, icon = Icons.Default.WaterDrop)
                                }
                                if (room.ownerName.isNotBlank() || room.ownerPhone.isNotBlank()) {
                                    RoomInfoRow(
                                        label = "Landlord / Owner",
                                        value = "${room.ownerName} (${room.ownerPhone})",
                                        icon = Icons.Default.Person
                                    )
                                }
                                if (room.address.isNotBlank()) {
                                    RoomInfoRow(label = "Address", value = room.address, icon = Icons.Default.LocationOn)
                                }
                            }
                        }
                    }

                    // Card 3: Roommates & Members List
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Roommates & Members (${members.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827)
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFEFF6FF)
                                    ) {
                                        Text(
                                            text = "All Synced",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D4ED8),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                members.forEach { member ->
                                    val isMe = member.id == currentUserId
                                    val isRoomAdmin = member.id == room.adminUid || member.role == UserRole.ADMIN

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF9FAFB),
                                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isRoomAdmin) Color(0xFF059669) else Color(0xFF2563EB)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = member.fullName.take(1).uppercase(),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = member.fullName,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF111827)
                                                        )
                                                        if (isMe) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("(You)", fontSize = 11.sp, color = Color(0xFF6B7280))
                                                        }
                                                    }
                                                    Text(
                                                        text = if (member.upiId.isNotBlank()) "UPI: ${member.upiId}" else "ID: ${member.sixDigitUserId}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF6B7280)
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isRoomAdmin) Color(0xFFECFDF5) else Color(0xFFEFF6FF)
                                            ) {
                                                Text(
                                                    text = if (isRoomAdmin) "Admin" else "Roommate",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isRoomAdmin) Color(0xFF065F46) else Color(0xFF1D4ED8),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Add Member via 6-digit Code input
                                Text("Add Member via 6-Digit Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = addMemberInput,
                                        onValueChange = { if (it.length <= 6) addMemberInput = it.filter { ch -> ch.isDigit() } },
                                        placeholder = { Text("6-digit code", fontSize = 12.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (addMemberInput.length == 6) {
                                                onAddMemberByCode(addMemberInput)
                                                Toast.makeText(context, "Added member with code $addMemberInput", Toast.LENGTH_SHORT).show()
                                                addMemberInput = ""
                                            } else {
                                                Toast.makeText(context, "Enter a valid 6-digit user code", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }

                    // Delete Room (if Admin)
                    if (isAdmin) {
                        item {
                            OutlinedButton(
                                onClick = onDeleteRoom,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete This Room", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomInfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: ", fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 12.sp, color = Color(0xFF111827), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
