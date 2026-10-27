package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PaymentMethod
import com.example.ui.theme.*
import com.example.viewmodel.VwsUiState

@Composable
fun RecordPaymentDialog(
    uiState: VwsUiState,
    onDismiss: () -> Unit,
    onSubmit: (
        toUserId: String,
        amountRupees: String,
        method: PaymentMethod,
        referenceId: String,
        notes: String
    ) -> Unit
) {
    val otherMembers = remember(uiState.users, uiState.currentUserId) {
        uiState.users.filter { it.id != uiState.currentUserId }
    }
    var toUserId by remember { mutableStateOf(otherMembers.firstOrNull()?.id ?: "") }
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var referenceId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var recipientDropdownExpanded by remember { mutableStateOf(false) }

    val toUser = otherMembers.find { it.id == toUserId }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = VwsSurface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vws_logo),
                            contentDescription = "Application Logo",
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "Record Room Payment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recipient Roommate
                Text("Pay To Roommate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VwsBorder, RoundedCornerShape(12.dp))
                        .clickable { recipientDropdownExpanded = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(toUser?.fullName ?: "Select Roommate", fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = recipientDropdownExpanded,
                        onDismissRequest = { recipientDropdownExpanded = false }
                    ) {
                        otherMembers.forEach { member ->
                            DropdownMenuItem(
                                text = { Text("${member.fullName} (${member.upiId})") },
                                onClick = {
                                    toUserId = member.id
                                    recipientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Payment Amount (₹)") },
                    placeholder = { Text("e.g. 500.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method
                Text("Payment Method", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.values().forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // UTR / Reference ID
                OutlinedTextField(
                    value = referenceId,
                    onValueChange = { referenceId = it },
                    label = { Text(if (selectedMethod == PaymentMethod.UPI) "UPI Ref / UTR (12-digit)" else "Payment Reference") },
                    placeholder = { Text("e.g. UPI/492019482710") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_ref_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note / Proof") },
                    placeholder = { Text("e.g. Settle EB & water bill share") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSubmit(toUserId, amountText, selectedMethod, referenceId, notes)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_payment_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit Payment", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
