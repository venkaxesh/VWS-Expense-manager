package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.NoteType
import com.example.model.User
import com.example.ui.theme.*
import com.example.ui.util.ReminderNotificationHelper

@Composable
fun AddNoteDialog(
    roommates: List<User> = emptyList(),
    currentUserId: String = "",
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        content: String,
        noteType: NoteType,
        isPrivate: Boolean,
        dueDateMillis: Long?,
        targetUserId: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var noteType by remember { mutableStateOf(NoteType.SHARED_ROOM) }
    var isPrivate by remember { mutableStateOf(false) }
    var hasDueDate by remember { mutableStateOf(false) }

    val otherRoommates = remember(roommates, currentUserId) {
        roommates.filter { it.id != currentUserId }
    }
    // null means "All Roommates", otherwise specific user id
    var selectedTargetUserId by remember { mutableStateOf<String?>(null) }

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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_vws_logo),
                                    contentDescription = "Kaasu Kudu Kaasu Logo",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text("Add Room Note", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                            Text("Sends notification tone to roommates", fontSize = 11.sp, color = Color(0xFF059669))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g. EB Bill Deadline, Settle Share") },
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details & Instructions") },
                    placeholder = { Text("Write message for roommates...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("note_content_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Send Notification To target selection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Notify Roommate(s) with Tone",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTargetUserId == null,
                            onClick = { selectedTargetUserId = null },
                            label = { Text("All Roommates", fontSize = 11.sp) }
                        )
                    }
                    items(otherRoommates) { member ->
                        FilterChip(
                            selected = selectedTargetUserId == member.id,
                            onClick = { selectedTargetUserId = member.id },
                            label = { Text(member.fullName.ifBlank { member.username }, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Note Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = noteType == NoteType.SHARED_ROOM,
                        onClick = { noteType = NoteType.SHARED_ROOM; isPrivate = false },
                        label = { Text("Shared Room", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = noteType == NoteType.PAYMENT_REMINDER,
                        onClick = { noteType = NoteType.PAYMENT_REMINDER },
                        label = { Text("Payment Alert", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = noteType == NoteType.DUE_DATE,
                        onClick = { noteType = NoteType.DUE_DATE; hasDueDate = true },
                        label = { Text("Due Date", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasDueDate,
                        onCheckedChange = { hasDueDate = it }
                    )
                    Text("Set deadline for 3 days from now", fontSize = 12.sp, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val due = if (hasDueDate) System.currentTimeMillis() + (3 * 86400000L) else null
                            val targetName = if (selectedTargetUserId != null) {
                                otherRoommates.firstOrNull { it.id == selectedTargetUserId }?.fullName ?: "Roommate"
                            } else "All Roommates"

                            ReminderNotificationHelper.sendReminderNotification(
                                context = context,
                                title = "Kaasu Kudu Kaasu: ${title.ifBlank { "Room Notice" }}",
                                message = "To $targetName: ${content.ifBlank { "New note logged for room." }}"
                            )
                            onSubmit(title, content, noteType, isPrivate, due, selectedTargetUserId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_note_btn")
                    ) {
                        Text("Save & Notify", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
