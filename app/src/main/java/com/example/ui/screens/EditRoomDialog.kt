package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CurrencyHelper
import com.example.model.Room
import com.example.viewmodel.VwsUiState

@Composable
fun EditRoomDialog(
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
        description: String
    ) -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    val context = LocalContext.current
    val currentRoom = uiState.currentRoom ?: return

    var name by remember { mutableStateOf(currentRoom.name) }
    var address by remember { mutableStateOf(currentRoom.address) }
    var ebNumber by remember { mutableStateOf(currentRoom.ebConsumerNumber) }
    var waterNumber by remember { mutableStateOf(currentRoom.waterTaxNumber) }
    var ownerName by remember { mutableStateOf(currentRoom.ownerName) }
    var ownerPhone by remember { mutableStateOf(currentRoom.ownerPhone) }
    var monthlyRent by remember {
        mutableStateOf(
            if (currentRoom.monthlyRentPaise > 0)
                (currentRoom.monthlyRentPaise / 100).toString()
            else ""
        )
    }
    var rentDueDay by remember { mutableStateOf(currentRoom.rentDueDay.toString()) }
    var description by remember { mutableStateOf(currentRoom.description) }

    var newMemberInput by remember { mutableStateOf("") }
    val userMap = remember(uiState.users) { uiState.users.associateBy { it.id } }
    val isCreatorOrAdmin = uiState.isCurrentAdmin || currentRoom.adminUid == uiState.currentUserId

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("dialog_edit_room"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Edit Room Settings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "Update room info, manage members & codes",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF6B7280))
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Invitation Code Badge
                    if (currentRoom.invitationCode.isNotBlank()) {
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "6-DIGIT ROOM INVITATION CODE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentRoom.invitationCode,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46),
                                        letterSpacing = 2.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", currentRoom.invitationCode))
                                        Toast.makeText(context, "Code copied: ${currentRoom.invitationCode}", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF059669)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669))
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Room Basic Info
                    Text(
                        text = "ROOM DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Room Name *") },
                        placeholder = { Text("e.g. Kaasu Kudulu Bachelor Pad") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF6B7280)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Flat Number") },
                        placeholder = { Text("e.g. 4B, Emerald Heights, Chennai") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF6B7280)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ebNumber,
                            onValueChange = { ebNumber = it },
                            label = { Text("EB Consumer #") },
                            placeholder = { Text("e.g. 03-12-445") },
                            leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF6B7280)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = waterNumber,
                            onValueChange = { waterNumber = it },
                            label = { Text("Water Tax #") },
                            placeholder = { Text("e.g. WTR-889") },
                            leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF6B7280)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = monthlyRent,
                            onValueChange = { monthlyRent = it.filter { char -> char.isDigit() } },
                            label = { Text("Monthly Rent (₹)") },
                            placeholder = { Text("e.g. 15000") },
                            leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF6B7280)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = rentDueDay,
                            onValueChange = { rentDueDay = it.filter { char -> char.isDigit() }.take(2) },
                            label = { Text("Due Day (1-31)") },
                            placeholder = { Text("5") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF6B7280)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner Name") },
                            placeholder = { Text("House Owner") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF6B7280)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = ownerPhone,
                            onValueChange = { ownerPhone = it },
                            label = { Text("Owner Phone") },
                            placeholder = { Text("Phone") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF6B7280)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes") },
                        placeholder = { Text("Additional room details, rules, or amenities...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Members Management Section
                    Text(
                        text = "ROOM MEMBERS (${uiState.members.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        letterSpacing = 0.5.sp
                    )

                    // Add Member by User ID / Username input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newMemberInput,
                            onValueChange = { newMemberInput = it },
                            label = { Text("User ID (e.g. USR-AB12) or Username") },
                            placeholder = { Text("Enter member User ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                if (newMemberInput.isNotBlank()) {
                                    onAddMember(newMemberInput)
                                    newMemberInput = ""
                                }
                            },
                            enabled = isCreatorOrAdmin && newMemberInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }

                    // Member list cards
                    uiState.members.forEach { member ->
                        val user = userMap[member.userId]
                        val isCreator = member.userId == currentRoom.adminUid
                        Surface(
                            color = Color(0xFFF9FAFB),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCreator) Color(0xFFFEF3C7) else Color(0xFFE0E7FF),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = (user?.fullName?.firstOrNull() ?: 'U').toString().uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isCreator) Color(0xFFB45309) else Color(0xFF4338CA)
                                            )
                                        }
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = user?.fullName ?: "Member (${member.userId.take(6)})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF111827)
                                            )
                                            if (isCreator) {
                                                Surface(
                                                    color = Color(0xFFFEF3C7),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Creator",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB45309),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "ID: ${user?.shareableUserId ?: "USR-${member.userId.takeLast(6).uppercase()}"}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }

                                if (isCreatorOrAdmin && !isCreator) {
                                    IconButton(
                                        onClick = { onRemoveMember(member.userId) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove Member",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            onSubmit(
                                name,
                                address,
                                ebNumber,
                                waterNumber,
                                ownerName,
                                ownerPhone,
                                monthlyRent,
                                rentDueDay.toIntOrNull() ?: currentRoom.rentDueDay,
                                description
                            )
                        },
                        enabled = name.isNotBlank() && isCreatorOrAdmin,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
