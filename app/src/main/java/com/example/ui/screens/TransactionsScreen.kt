package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.VwsUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: VwsUiState,
    onLaunchUpi: (payeeUpi: String, payeeName: String, amountPaise: Long, note: String) -> Unit,
    onVerifyPayment: (paymentId: String, newStatus: PaymentStatus, note: String) -> Unit,
    onRecordNewPayment: () -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: My Transactions, 1: Room Transactions
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<PaymentStatus?>(null) }

    val userMap = uiState.users.associateBy { it.id }
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Tab Selector matching clean minimalist design
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.White,
            contentColor = Color(0xFF111827)
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("My Transactions", fontWeight = FontWeight.Bold, color = if (selectedSubTab == 0) Color(0xFF111827) else Color(0xFF6B7280)) },
                modifier = Modifier.testTag("tab_my_transactions")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Room Transactions", fontWeight = FontWeight.Bold, color = if (selectedSubTab == 1) Color(0xFF111827) else Color(0xFF6B7280)) },
                modifier = Modifier.testTag("tab_room_transactions")
            )
        }

        // Search & Pay button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions...", fontSize = 13.sp, color = Color(0xFF9CA3AF)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6B7280)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("transaction_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRecordNewPayment,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(48.dp).testTag("record_payment_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pay", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Settlement recommendations
        if (uiState.settlementSuggestions.isNotEmpty() && selectedSubTab == 0) {
            val mySettlements = uiState.settlementSuggestions.filter {
                it.fromUserId == uiState.currentUserId || it.toUserId == uiState.currentUserId
            }
            if (mySettlements.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "SETTLEMENT SUGGESTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    items(mySettlements) { s ->
                        Surface(
                            color = Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val isIOwe = s.fromUserId == uiState.currentUserId
                                    Text(
                                        text = if (isIOwe) "You owe ${s.toUserName}" else "${s.fromUserName} owes you",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF111827)
                                    )
                                    Text(
                                        text = "UPI: ${s.toUserUpiId.ifBlank { "Not configured" }}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = CurrencyHelper.formatPaiseToRupee(s.amountPaise),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF111827)
                                    )
                                    if (s.fromUserId == uiState.currentUserId && s.toUserUpiId.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                onLaunchUpi(
                                                    s.toUserUpiId,
                                                    s.toUserName,
                                                    s.amountPaise,
                                                    "VWS Room settlement"
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                                            modifier = Modifier.testTag("upi_pay_btn_${s.toUserId}")
                                        ) {
                                            Text("Pay UPI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transactions list matching Screenshot 4
        val filteredPayments = uiState.payments.filter { payment ->
            val matchesTab = if (selectedSubTab == 0) {
                payment.fromUserId == uiState.currentUserId || payment.toUserId == uiState.currentUserId
            } else {
                true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val fromName = userMap[payment.fromUserId]?.fullName ?: ""
                val toName = userMap[payment.toUserId]?.fullName ?: ""
                payment.referenceId.contains(searchQuery, ignoreCase = true) ||
                        fromName.contains(searchQuery, ignoreCase = true) ||
                        toName.contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesSearch
        }

        if (filteredPayments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No transactions found", fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Text("Record a payment to track settlement history.", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(filteredPayments) { payment ->
                    val fromUser = userMap[payment.fromUserId]
                    val toUser = userMap[payment.toUserId]
                    val isPayer = payment.fromUserId == uiState.currentUserId
                    val isRecipient = payment.toUserId == uiState.currentUserId
                    val canVerify = (uiState.isCurrentAdmin || isRecipient) && payment.status == PaymentStatus.PAYMENT_SUBMITTED

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_item_${payment.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular user avatar matching Screenshot 4
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (fromUser?.fullName?.firstOrNull() ?: 'U').toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF111827)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isPayer) "To: ${toUser?.fullName ?: "Member"}" else fromUser?.fullName ?: "Member",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF111827) // Solid Black text
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(payment.status)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${payment.paymentMethod.displayName} • ${dateFormat.format(Date(payment.submittedAtMillis))}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }

                                Text(
                                    text = CurrencyHelper.formatPaiseToRupee(payment.amountPaise),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (isRecipient) Color(0xFF15803D) else Color(0xFF111827) // Green if received, black if paid
                                )
                            }

                            if (payment.referenceId.isNotBlank() || payment.proofNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ref: ${payment.referenceId.ifBlank { "N/A" }} ${if (payment.proofNotes.isNotBlank()) "• ${payment.proofNotes}" else ""}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }

                            if (canVerify) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { onVerifyPayment(payment.id, PaymentStatus.DISPUTED, "Disputed") },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                    ) {
                                        Text("Dispute", fontSize = 11.sp, color = Color(0xFFDC2626))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onVerifyPayment(payment.id, PaymentStatus.PAID, "Confirmed") },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                    ) {
                                        Text("Confirm Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
