package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.theme.*
import com.example.viewmodel.VwsUiState

@Composable
fun CreateRoomDialog(
    uiState: VwsUiState,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        address: String,
        ebNumber: String,
        waterNumber: String,
        ownerName: String,
        ownerPhone: String,
        monthlyRentRupee: String,
        rentDueDay: Int,
        description: String,
        selectedMemberIds: List<String>
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var monthlyRent by remember { mutableStateOf("") }
    var rentDueDay by remember { mutableStateOf("5") }
    var showOptionalDetails by remember { mutableStateOf(false) }

    var address by remember { mutableStateOf("") }
    var ebNumber by remember { mutableStateOf("") }
    var waterNumber by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = VwsSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_vws_logo),
                                    contentDescription = "VWS Logo",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text("Create New Room", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                            Text("Instant setup • 6-digit room code", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFF991B1B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (it.isNotBlank()) errorMessage = null
                            },
                            label = { Text("Room Name / Flat No *") },
                            placeholder = { Text("e.g. Flat 401 — Green Valley") },
                            modifier = Modifier.fillMaxWidth().testTag("create_room_name"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = monthlyRent,
                                onValueChange = { monthlyRent = it.filter { c -> c.isDigit() } },
                                label = { Text("Monthly Rent (₹)") },
                                placeholder = { Text("e.g. 25000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = rentDueDay,
                                onValueChange = { rentDueDay = it.filter { c -> c.isDigit() } },
                                label = { Text("Due Day (1-31)") },
                                placeholder = { Text("5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        // Collapsible Section for optional details
                        Surface(
                            color = VwsSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOptionalDetails = !showOptionalDetails }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showOptionalDetails) "Hide Optional Utility Details" else "+ Add Utility & Owner Details (Optional)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldDark
                                )
                                Icon(
                                    imageVector = if (showOptionalDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (showOptionalDetails) {
                        item {
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Full Address (Optional)") },
                                placeholder = { Text("e.g. 12th Main, Koramangala, Bengaluru") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = ebNumber,
                                    onValueChange = { ebNumber = it },
                                    label = { Text("EB Consumer #") },
                                    placeholder = { Text("e.g. EB-109283") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = waterNumber,
                                    onValueChange = { waterNumber = it },
                                    label = { Text("Water Tax #") },
                                    placeholder = { Text("e.g. WTR-88219") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = { ownerName = it },
                                    label = { Text("Owner Name") },
                                    placeholder = { Text("Mr. Ramesh") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = ownerPhone,
                                    onValueChange = { ownerPhone = it },
                                    label = { Text("Owner Phone") },
                                    placeholder = { Text("+91 98450...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Room Description / Notes") },
                                placeholder = { Text("e.g. 3BHK bachelor accommodation") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Please enter a Room Name"
                                return@Button
                            }
                            onSubmit(
                                name, address, ebNumber, waterNumber, ownerName, ownerPhone,
                                monthlyRent, rentDueDay.toIntOrNull() ?: 5, description,
                                listOf(uiState.currentUserId)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("confirm_create_room_btn")
                    ) {
                        Text("Create Room", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
