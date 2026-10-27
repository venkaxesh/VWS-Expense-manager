package com.example.ui.screens

import com.example.model.UserRole

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.CurrencyHelper
import com.example.model.Expense
import com.example.model.ExpenseSplit
import com.example.model.User
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.*
import com.example.viewmodel.VwsUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseDetailDialog(
    expense: Expense,
    uiState: VwsUiState,
    onDismiss: () -> Unit,
    onDirectPayGooglePay: (payeeUser: User, amountPaise: Long, note: String) -> Unit = { _, _, _ -> },
    onDirectPayCash: (payeeUserId: String, amountPaise: Long, splitId: String) -> Unit = { _, _, _ -> }
) {
    val userMap = uiState.users.associateBy { it.id }
    val payerUser = userMap[expense.payerUserId]
    val splits = uiState.splits.filter { it.expenseId == expense.id }
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())

    // If the logged in user has a split in this expense
    val mySplit = splits.find { it.memberUserId == uiState.currentUserId }
    // User can click any split in the list to select it for direct payment
    var selectedSplitForPayment by remember {
        mutableStateOf<ExpenseSplit?>(
            if (mySplit != null && !mySplit.isPaid && mySplit.memberUserId != expense.payerUserId) mySplit
            else splits.firstOrNull { !it.isPaid && it.memberUserId != expense.payerUserId }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(22.dp))
                .testTag("dialog_expense_detail"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Application Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_vws_logo),
                                    contentDescription = "Application Logo",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = expense.description,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = expense.category.displayName,
                                fontSize = 12.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF6B7280))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Bill Summary Card
                    item {
                        Surface(
                            color = Color(0xFFF9FAFB),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Bill Amount", fontSize = 13.sp, color = Color(0xFF6B7280))
                                    Text(
                                        text = CurrencyHelper.formatPaiseToRupee(expense.totalAmountPaise),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF111827)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Paid Upfront By", fontSize = 13.sp, color = Color(0xFF6B7280))
                                    Text(
                                        text = "${payerUser?.fullName ?: "Member"} (Credited)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF059669)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Split Strategy", fontSize = 13.sp, color = Color(0xFF6B7280))
                                    Text(
                                        text = expense.splitType.displayName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = Color(0xFF374151)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Date & Time", fontSize = 12.sp, color = Color(0xFF6B7280))
                                    Text(
                                        text = dateFormat.format(Date(expense.expenseDateMillis)),
                                        fontSize = 12.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    }

                    // Direct Pay Split Card (Requested: Split payment click to open a Split amount to direct pay with google pay or cash)
                    selectedSplitForPayment?.let { activeSplit ->
                        val member = userMap[activeSplit.memberUserId]
                        val isPayer = activeSplit.memberUserId == expense.payerUserId

                        if (!isPayer && !activeSplit.isPaid) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.5.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Payment,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Direct Pay Split Amount",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E40AF)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFDBEAFE)
                                            ) {
                                                Text(
                                                    text = member?.fullName ?: "Member",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1D4ED8),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Payable Split Amount", fontSize = 11.sp, color = Color(0xFF4B5563))
                                                Text(
                                                    text = CurrencyHelper.formatPaiseToRupee(activeSplit.allocatedAmountPaise),
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF111827)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Payee", fontSize = 11.sp, color = Color(0xFF4B5563))
                                                Text(
                                                    text = payerUser?.fullName ?: "Payer",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF111827)
                                                )
                                                if (!payerUser?.upiId.isNullOrBlank()) {
                                                    Text(
                                                        text = "UPI: ${payerUser?.upiId}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF059669),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Two direct payment action buttons: Google Pay and Cash
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 1. Google Pay button
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
                                                    onDirectPayGooglePay(
                                                        payee,
                                                        activeSplit.allocatedAmountPaise,
                                                        "VWS Split: ${expense.description}"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("btn_direct_pay_google_pay")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Google Pay", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            // 2. Direct Cash payment button
                                            OutlinedButton(
                                                onClick = {
                                                    onDirectPayCash(
                                                        expense.payerUserId,
                                                        activeSplit.allocatedAmountPaise,
                                                        activeSplit.id
                                                    )
                                                },
                                                border = BorderStroke(1.dp, Color(0xFF059669)),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("btn_direct_pay_cash")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachMoney,
                                                    contentDescription = null,
                                                    tint = Color(0xFF059669),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pay via Cash", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section Title: Member Allocations (Tap to direct pay)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MEMBER SPLITS (${splits.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B7280),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Tap any split to direct pay",
                                fontSize = 11.sp,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    items(splits) { split ->
                        val member = userMap[split.memberUserId]
                        val isPayer = split.memberUserId == expense.payerUserId
                        val isSelected = selectedSplitForPayment?.id == split.id

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF0FDF4) else Color(0xFFFAFAFA)
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) Color(0xFF10B981) else Color(0xFFE5E7EB)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isPayer) {
                                        selectedSplitForPayment = split
                                    }
                                }
                                .testTag("split_item_${split.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isPayer) Color(0xFFECFDF5) else if (split.isPaid) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = (member?.fullName?.firstOrNull() ?: 'M').toString().uppercase(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPayer) Color(0xFF059669) else if (split.isPaid) Color(0xFF047857) else Color(0xFFB45309)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = member?.fullName ?: "Roommate",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF111827)
                                        )
                                        Text(
                                            text = if (isPayer) "Original Payer (Credited)" else if (split.isPaid) "Settled" else "Pending (Tap to Pay)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isPayer || split.isPaid) Color(0xFF059669) else Color(0xFFD97706)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = CurrencyHelper.formatPaiseToRupee(split.allocatedAmountPaise),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF111827)
                                    )
                                    if (!isPayer && !split.isPaid) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Pay",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (expense.notes.isNotBlank()) {
                        item {
                            Surface(
                                color = Color(0xFFF9FAFB),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Notes: ${expense.notes}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4B5563),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
