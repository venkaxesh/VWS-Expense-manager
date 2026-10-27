package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CurrencyHelper
import com.example.model.ExpenseCategory
import com.example.model.SplitType
import com.example.model.User
import com.example.ui.theme.*
import com.example.viewmodel.VwsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    uiState: VwsUiState,
    onDismiss: () -> Unit,
    onSubmit: (
        description: String,
        amountRupees: String,
        category: ExpenseCategory,
        customCategory: String,
        splitType: SplitType,
        payerUserId: String,
        expenseDateMillis: Long,
        selectedMemberIds: List<String>,
        splitInputs: Map<String, String>,
        notes: String
    ) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.ESSENTIALS) }
    var customCategoryText by remember { mutableStateOf("") }
    var selectedSplitType by remember { mutableStateOf(SplitType.EQUAL) }
    var payerUserId by remember { mutableStateOf(uiState.currentUserId) }
    var notes by remember { mutableStateOf("") }

    val activeMemberUsers = remember(uiState.members, uiState.users) {
        val activeIds = uiState.members.filter { it.status.name == "ACTIVE" }.map { it.userId }
        uiState.users.filter { activeIds.contains(it.id) }
    }
    var selectedMemberIds by remember { mutableStateOf(activeMemberUsers.map { it.id }.toSet()) }
    var splitInputs by remember { mutableStateOf(mapOf<String, String>()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header matching Screenshot 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vws_logo),
                            contentDescription = "Application Logo",
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF111827)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF111827))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Big Centered Amount Display matching Screenshot 3
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "₹ ",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                                    placeholder = { Text("0", fontSize = 32.sp, color = Color(0xFF9CA3AF)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.Start
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .widthIn(min = 100.dp, max = 220.dp)
                                        .testTag("expense_amount_input")
                                )
                            }
                            // Underline under amount matching Screenshot 3
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(3.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    // Description text input
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description", color = Color(0xFF6B7280)) },
                            placeholder = { Text("e.g. BESCOM bill, groceries, Wi-Fi", color = Color(0xFF9CA3AF)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_description_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Paid by: Horizontal Avatar Row matching Screenshot 3
                    item {
                        Text(
                            text = "Paid by",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeMemberUsers) { member ->
                                val isSelected = member.id == payerUserId
                                val initial = member.fullName.firstOrNull()?.toString() ?: "U"
                                val isMe = member.id == uiState.currentUserId
                                val displayName = if (isMe) "You" else member.fullName.split(" ").firstOrNull() ?: member.fullName

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { payerUserId = member.id }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) Color(0xFF0F172A) else Color(0xFFF3F4F6)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE5E7EB),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initial,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (isSelected) Color.White else Color(0xFF111827)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color(0xFF111827)
                                    )
                                }
                            }
                        }
                    }

                    // Split Method Segmented Pill matching Screenshot 3
                    item {
                        Text(
                            text = "Split method",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    SplitType.EQUAL to "Equal",
                                    SplitType.PERCENTAGE to "Percentage",
                                    SplitType.RATIO to "Ratio",
                                    SplitType.SEPARATE_AMOUNT to "Exact"
                                ).forEach { (type, label) ->
                                    val isSelected = selectedSplitType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color.White else Color.Transparent)
                                            .clickable { selectedSplitType = type }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color(0xFF111827) else Color(0xFF6B7280)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Category selection
                    item {
                        Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ExpenseCategory.values()) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.displayName, fontSize = 11.sp, color = Color(0xFF111827)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFF3F4F6)
                                    )
                                )
                            }
                        }
                    }

                    // Roommate inclusions with manual inputs if needed
                    item {
                        Text("Include Room Members", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                    }

                    items(activeMemberUsers) { member ->
                        val isChecked = selectedMemberIds.contains(member.id)
                        Surface(
                            color = if (isChecked) Color(0xFFF9FAFB) else Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedMemberIds = if (checked) {
                                            selectedMemberIds + member.id
                                        } else {
                                            if (selectedMemberIds.size > 1) selectedMemberIds - member.id else selectedMemberIds
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.fullName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF111827))
                                    Text("@${member.username}", fontSize = 11.sp, color = Color(0xFF6B7280))
                                }

                                if (isChecked && selectedSplitType != SplitType.EQUAL) {
                                    val inputVal = splitInputs[member.id] ?: ""
                                    OutlinedTextField(
                                        value = inputVal,
                                        onValueChange = { newVal ->
                                            splitInputs = splitInputs + (member.id to newVal)
                                        },
                                        placeholder = {
                                            Text(
                                                when (selectedSplitType) {
                                                    SplitType.RATIO -> "1"
                                                    SplitType.PERCENTAGE -> "25%"
                                                    SplitType.SEPARATE_AMOUNT -> "₹"
                                                    else -> ""
                                                },
                                                fontSize = 11.sp
                                            )
                                        },
                                        modifier = Modifier.width(85.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Optional Notes") },
                            placeholder = { Text("e.g. Verified by meter reading") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Button matching Screenshot 3: Dark Pill "Add expense"
                Button(
                    onClick = {
                        onSubmit(
                            description,
                            amountText,
                            selectedCategory,
                            customCategoryText,
                            selectedSplitType,
                            payerUserId,
                            System.currentTimeMillis(),
                            selectedMemberIds.toList(),
                            splitInputs,
                            notes
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_expense_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)), // Dark navy/black matching Screenshot 3
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add expense", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}
