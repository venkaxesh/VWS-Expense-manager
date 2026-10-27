package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CurrencyHelper
import com.example.model.MemberStatus
import com.example.ui.theme.*
import com.example.viewmodel.VwsUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardDialog(
    uiState: VwsUiState,
    onDismiss: () -> Unit,
    onApproveMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onAddAdjustment: (description: String, amountRupees: String) -> Unit,
    onDeleteRoom: (String) -> Unit = {},
    onAddMemberByCode: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Room & Members, 1: Audit Trail, 2: Financial Adjustment
    var adjDesc by remember { mutableStateOf("") }
    var adjAmount by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var newMemberInput by remember { mutableStateOf("") }

    val userMap = uiState.users.associateBy { it.id }
    val currentRoom = uiState.currentRoom
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    if (showDeleteConfirmDialog && currentRoom != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("Delete Room", fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            },
            text = {
                Text("Are you sure you want to permanently delete '${currentRoom.name}'? All associated room expenses, splits, and member records will be removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteRoom(currentRoom.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Room", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = VwsSurface)
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
                            contentDescription = "VWS Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Admin Console", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                            Text("Room: ${currentRoom?.name ?: "Flat 302"}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Members & Utilities, Audit Logs, Adjustments
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = VwsSurfaceVariant,
                    contentColor = EmeraldPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Members & Utilities", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Audit Trail", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Adjustment", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> {
                        // Room details + Member approvals
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Utility Info Card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = VwsSurfaceVariant),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("UTILITIES & RENT DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("EB Consumer No: ${currentRoom?.ebConsumerNumber ?: "EB-BLR-098234"}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("Water Tax No: ${currentRoom?.waterTaxNumber ?: "WTR-77218"}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("Owner: ${currentRoom?.ownerName ?: "Mr. K. Sharma"} (${currentRoom?.ownerPhone ?: "+91 9845012345"})", fontSize = 12.sp)
                                        Text("Monthly Rent: ${CurrencyHelper.formatPaiseToRupee(currentRoom?.monthlyRentPaise ?: 2400000L)} (Due: ${currentRoom?.rentDueDay ?: 5}th of month)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EmeraldDark)
                                    }
                                }
                            }

                            // Room Share & Member Add Card
                            if (currentRoom != null) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("ROOM INVITATION CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                                                    Text(currentRoom.invitationCode.ifBlank { "N/A" }, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", currentRoom.invitationCode))
                                                            Toast.makeText(context, "Copied Room Code: ${currentRoom.invitationCode}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            val joinUrl = "https://ais-pre-5nwglpxieuwh7axszorc7n-264640036918.asia-southeast1.run.app?room=${currentRoom.invitationCode}"
                                                            val shareMsg = "Join our room '${currentRoom.name}' on Flatmates Splitwise!\nUse 6-digit Room Code: ${currentRoom.invitationCode}\nDirect Link to Join: $joinUrl"
                                                            val sendIntent = Intent().apply {
                                                                action = Intent.ACTION_SEND
                                                                putExtra(Intent.EXTRA_TEXT, shareMsg)
                                                                type = "text/plain"
                                                            }
                                                            context.startActivity(Intent.createChooser(sendIntent, "Share Room Invitation"))
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Share Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Text("Add Member via 6-Digit User ID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = newMemberInput,
                                                    onValueChange = { newMemberInput = it },
                                                    placeholder = { Text("6-digit User ID or username", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                                )
                                                Button(
                                                    onClick = {
                                                        if (newMemberInput.isNotBlank()) {
                                                            onAddMemberByCode(newMemberInput.trim())
                                                            newMemberInput = ""
                                                        } else {
                                                            Toast.makeText(context, "Enter a 6-digit User ID", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                                                ) {
                                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Members List
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ROOM MEMBERS (${uiState.members.size}/6 Max)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                }
                            }

                            items(uiState.members) { rm ->
                                val user = userMap[rm.userId]
                                val isPending = rm.status == MemberStatus.PENDING_APPROVAL

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = VwsSurface),
                                    border = CardDefaults.outlinedCardBorder(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(user?.fullName ?: rm.userId, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                if (rm.role.name == "ADMIN") {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(color = EmeraldContainer, shape = RoundedCornerShape(4.dp)) {
                                                        Text("Admin", fontSize = 9.sp, color = EmeraldDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "6-Digit ID: ${user?.sixDigitUserId ?: "N/A"} • @${user?.username}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EmeraldDark
                                            )
                                            Text("UPI: ${user?.upiId} • Bank: ${user?.maskedAccountNumber}", fontSize = 10.sp, color = TextMuted)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Copy user id
                                            IconButton(
                                                onClick = {
                                                    val idToCopy = user?.sixDigitUserId ?: ""
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("User ID", idToCopy))
                                                    Toast.makeText(context, "Copied ID: $idToCopy", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                            }

                                            if (isPending) {
                                                FilledTonalButton(
                                                    onClick = { onApproveMember(rm.userId) },
                                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = PaidGreen, contentColor = Color.White),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else if (rm.role.name != "ADMIN") {
                                                IconButton(onClick = { onRemoveMember(rm.userId) }) {
                                                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = OverdueRed, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (currentRoom != null && (uiState.isCurrentAdmin || currentRoom.adminUid == uiState.currentUserId)) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "DANGER ZONE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Permanently delete '${currentRoom.name}'. This will remove all associated expenses, splits, and member records.",
                                                fontSize = 12.sp,
                                                color = Color(0xFF7F1D1D)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedButton(
                                                onClick = { showDeleteConfirmDialog = true },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Delete Room", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Audit Trail Viewer
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text("FINANCIAL CORRECTIONS & AUDIT TRAIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            }
                            items(uiState.auditLogs) { log ->
                                val performer = userMap[log.performedByUserId]
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = VwsSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(log.actionType, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EmeraldDark)
                                            Text(dateFormat.format(Date(log.timestampMillis)), fontSize = 10.sp, color = TextMuted)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(log.description, fontSize = 12.sp, color = TextPrimary)
                                        Text("By: ${performer?.fullName ?: log.performedByUserId}", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Financial Adjustment
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Record Auditable Adjustment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Admins can adjust financial balances (e.g., owner deposit refund, maid bonus, or dispute correction). Never silently deleted — all entries are logged permanently.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            OutlinedTextField(
                                value = adjAmount,
                                onValueChange = { adjAmount = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Adjustment Amount in ₹") },
                                placeholder = { Text("e.g. 500") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = adjDesc,
                                onValueChange = { adjDesc = it },
                                label = { Text("Reason for Correction / Adjustment") },
                                placeholder = { Text("e.g. Electricity bill refund credited from landlord") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    onAddAdjustment(adjDesc, adjAmount)
                                    adjDesc = ""
                                    adjAmount = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Auditable Adjustment")
                            }
                        }
                    }
                }
            }
        }
    }
}
