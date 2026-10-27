package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CurrencyHelper
import com.example.model.Expense
import com.example.model.ExpenseCategory
import com.example.ui.components.CategoryIconBadge
import com.example.ui.theme.*
import com.example.viewmodel.VwsUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarReportDialog(
    uiState: VwsUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDay by remember { mutableIntStateOf(calendarMonth.get(Calendar.DAY_OF_MONTH)) }
    var categoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val userMap = uiState.users.associateBy { it.id }

    // Filter expenses for this month
    val currentMonthExpenses = remember(calendarMonth, uiState.expenses, categoryFilter) {
        val cal = Calendar.getInstance()
        uiState.expenses.filter { exp ->
            cal.timeInMillis = exp.expenseDateMillis
            val sameMonth = cal.get(Calendar.MONTH) == calendarMonth.get(Calendar.MONTH) &&
                    cal.get(Calendar.YEAR) == calendarMonth.get(Calendar.YEAR)
            val matchesCategory = categoryFilter == null || exp.category == categoryFilter
            sameMonth && matchesCategory
        }
    }

    val totalRoomMonthPaise = currentMonthExpenses.sumOf { it.totalAmountPaise }
    val myMonthAllocatedPaise = remember(currentMonthExpenses, uiState.splits, uiState.currentUserId) {
        val expIds = currentMonthExpenses.map { it.id }.toSet()
        uiState.splits.filter { expIds.contains(it.expenseId) && it.memberUserId == uiState.currentUserId }
            .sumOf { it.allocatedAmountPaise }
    }
    val myMonthPaymentsPaise = remember(calendarMonth, uiState.payments, uiState.currentUserId) {
        val cal = Calendar.getInstance()
        uiState.payments.filter { pay ->
            cal.timeInMillis = pay.submittedAtMillis
            val sameMonth = cal.get(Calendar.MONTH) == calendarMonth.get(Calendar.MONTH) &&
                    cal.get(Calendar.YEAR) == calendarMonth.get(Calendar.YEAR)
            pay.fromUserId == uiState.currentUserId && pay.status.name == "PAID"
        }.sumOf { it.amountPaise }
    }
    val myMonthOutstandingPaise = if (myMonthAllocatedPaise > myMonthPaymentsPaise) (myMonthAllocatedPaise - myMonthPaymentsPaise) else 0L

    // Days with expenses set
    val daysWithExpenses = remember(currentMonthExpenses) {
        val cal = Calendar.getInstance()
        currentMonthExpenses.map { exp ->
            cal.timeInMillis = exp.expenseDateMillis
            cal.get(Calendar.DAY_OF_MONTH)
        }.toSet()
    }

    // Filter expenses for selected day
    val selectedDayExpenses = remember(selectedDay, currentMonthExpenses) {
        val cal = Calendar.getInstance()
        currentMonthExpenses.filter { exp ->
            cal.timeInMillis = exp.expenseDateMillis
            cal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    val categoryTotals = remember(currentMonthExpenses) {
        ExpenseCategory.values().mapNotNull { cat ->
            val sum = currentMonthExpenses.filter { it.category == cat }.sumOf { it.totalAmountPaise }
            if (sum > 0L) Pair(cat, sum) else null
        }.sortedByDescending { it.second }
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
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header with Month switcher matching Screenshot 5
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = calendarMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        calendarMonth = newCal
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month", tint = Color(0xFF111827))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vws_logo),
                            contentDescription = "Application Logo",
                            modifier = Modifier.size(30.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthFormat.format(calendarMonth.time),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color(0xFF111827) // Solid Black
                            )
                            Text(
                                text = "Room Expense Calendar",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    IconButton(onClick = {
                        val newCal = calendarMonth.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        calendarMonth = newCal
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = Color(0xFF111827))
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF111827))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Weekday headers: M T W T F S S
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                                Text(
                                    text = dayLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B7280),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Real-time 7-Column Monthly Calendar Grid
                    item {
                        val firstDayCal = (calendarMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
                        val dayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
                        val emptySlotsBefore = when (dayOfWeek) {
                            Calendar.MONDAY -> 0
                            Calendar.TUESDAY -> 1
                            Calendar.WEDNESDAY -> 2
                            Calendar.THURSDAY -> 3
                            Calendar.FRIDAY -> 4
                            Calendar.SATURDAY -> 5
                            Calendar.SUNDAY -> 6
                            else -> 0
                        }
                        val daysInMonth = calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val totalCells = emptySlotsBefore + daysInMonth
                        val numRows = (totalCells + 6) / 7

                        val todayCal = Calendar.getInstance()
                        val isCurrentMonthAndYear = todayCal.get(Calendar.YEAR) == calendarMonth.get(Calendar.YEAR) &&
                                todayCal.get(Calendar.MONTH) == calendarMonth.get(Calendar.MONTH)
                        val todayDay = if (isCurrentMonthAndYear) todayCal.get(Calendar.DAY_OF_MONTH) else -1
                        val rentDueDay = uiState.currentRoom?.rentDueDay ?: 5

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (row in 0 until numRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (col in 0..6) {
                                        val cellIndex = row * 7 + col
                                        val day = cellIndex - emptySlotsBefore + 1
                                        if (day in 1..daysInMonth) {
                                            val isSelected = day == selectedDay
                                            val isToday = day == todayDay
                                            val isRentDue = day == rentDueDay
                                            val hasExpense = daysWithExpenses.contains(day)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> Color(0xFF111827)
                                                            isToday -> Color(0xFFEFF6FF)
                                                            isRentDue -> Color(0xFFFEF3C7)
                                                            else -> Color(0xFFF9FAFB)
                                                        }
                                                    )
                                                    .clickable { selectedDay = day }
                                                    .border(
                                                        width = if (isSelected || isToday || isRentDue) 1.5.dp else 1.dp,
                                                        color = when {
                                                            isSelected -> Color(0xFF111827)
                                                            isToday -> Color(0xFF2563EB)
                                                            isRentDue -> Color(0xFFF59E0B)
                                                            else -> Color(0xFFE5E7EB)
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "$day",
                                                        fontWeight = if (isSelected || isToday || isRentDue) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 12.sp,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            isToday -> Color(0xFF1D4ED8)
                                                            isRentDue -> Color(0xFFB45309)
                                                            else -> Color(0xFF1F2937)
                                                        }
                                                    )
                                                    if (hasExpense) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) Color(0xFF10B981) else Color(0xFF059669))
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            // Calendar Legend
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Today", fontSize = 11.sp, color = Color(0xFF4B5563))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rent Due (Day $rentDueDay)", fontSize = 11.sp, color = Color(0xFF4B5563))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expenses", fontSize = 11.sp, color = Color(0xFF4B5563))
                                }
                            }
                        }
                    }

                    // Monthly summary Card matching Screenshot 5 exactly!
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Monthly summary",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF111827) // Solid Black
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                SummaryMetricRow("Total room expenses", CurrencyHelper.formatPaiseToRupee(totalRoomMonthPaise), Color(0xFF111827))
                                SummaryMetricRow("Your allocated share", CurrencyHelper.formatPaiseToRupee(myMonthAllocatedPaise), Color(0xFF111827))
                                SummaryMetricRow("Your payments", CurrencyHelper.formatPaiseToRupee(myMonthPaymentsPaise), Color(0xFF111827))
                                SummaryMetricRow("Your outstanding balance", CurrencyHelper.formatPaiseToRupee(myMonthOutstandingPaise), Color(0xFFDC2626)) // Bold Red for outstanding balance
                            }
                        }
                    }

                    // Selected Date Expenses
                    item {
                        Text(
                            text = "EXPENSES ON DAY $selectedDay (${selectedDayExpenses.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF)
                        )
                    }

                    if (selectedDayExpenses.isEmpty()) {
                        item {
                            Surface(
                                color = Color(0xFFF9FAFB),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No bills or expenses recorded on this date.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280),
                                    modifier = Modifier.padding(14.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(selectedDayExpenses) { exp ->
                            val payer = userMap[exp.payerUserId]
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIconBadge(exp.category, size = 36)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exp.description, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF111827))
                                        Text("Payer: ${payer?.fullName ?: "Member"}", fontSize = 11.sp, color = Color(0xFF6B7280))
                                    }
                                    Text(
                                        text = CurrencyHelper.formatPaiseToRupee(exp.totalAmountPaise),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF111827)
                                    )
                                }
                            }
                        }
                    }

                    // Category Breakdown for the Month
                    item {
                        Text("CATEGORY BREAKDOWN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(categoryTotals) { (cat, amount) ->
                        val pct = if (totalRoomMonthPaise > 0L) (amount.toFloat() / totalRoomMonthPaise.toFloat()) else 0f
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                                Text(CurrencyHelper.formatPaiseToRupee(amount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = pct,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF047857),
                                trackColor = Color(0xFFF3F4F6)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Export CSV Button
                Button(
                    onClick = {
                        val csvContent = buildString {
                            appendLine("Date,Description,Category,Total Amount (INR),Payer,Split Type")
                            currentMonthExpenses.forEach { exp ->
                                val payerName = userMap[exp.payerUserId]?.fullName ?: exp.payerUserId
                                val amountRupee = exp.totalAmountPaise / 100.0
                                appendLine("${dayFormat.format(Date(exp.expenseDateMillis))},\"${exp.description}\",${exp.category.displayName},$amountRupee,\"$payerName\",${exp.splitType.displayName}")
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "VWS Expense Report - ${monthFormat.format(calendarMonth.time)}")
                            putExtra(Intent.EXTRA_TEXT, csvContent)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Monthly Report"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_csv_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Monthly CSV Report", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}
