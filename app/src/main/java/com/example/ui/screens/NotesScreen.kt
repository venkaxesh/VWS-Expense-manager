package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteType
import com.example.model.RoomNote
import com.example.ui.theme.*
import com.example.viewmodel.VwsUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(
    uiState: VwsUiState,
    onAddNoteClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<NoteType?>(null) }
    val userMap = uiState.users.associateBy { it.id }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dueDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Filter notes: only show shared notes or notes where current user is author or target
    val visibleNotes = uiState.notes.filter { note ->
        val canView = !note.isPrivate || note.authorUserId == uiState.currentUserId || note.targetUserId == uiState.currentUserId || uiState.isCurrentAdmin
        val matchesType = selectedFilter == null || note.noteType == selectedFilter
        canView && matchesType
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VwsBgLight)
    ) {
        // Reminder Tone & Sound Preview Banner
        val context = androidx.compose.ui.platform.LocalContext.current
        Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("banner_reminder_tone")
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Reminder Tone (MP3) Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "Plays custom VWS sound for reminders & deadlines",
                            fontSize = 11.sp,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = {
                            com.example.ui.util.ReminderNotificationHelper.playReminderTone(context)
                            android.widget.Toast.makeText(context, "Playing VWS Reminder Tone", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFDBEAFE), contentColor = Color(0xFF1D4ED8)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_play_tone_notes")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play Tone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }


        if (visibleNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.StickyNote2, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No notes found", fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Text("Add a room notice, payment reminder, or flat rule.", fontSize = 12.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddNoteClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Note")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(visibleNotes) { note ->
                    val author = userMap[note.authorUserId]
                    val isAuthor = note.authorUserId == uiState.currentUserId
                    val isOverdue = note.dueDateMillis != null && note.dueDateMillis < System.currentTimeMillis()

                    val (badgeBg, badgeText, badgeIcon) = when (note.noteType) {
                        NoteType.SHARED_ROOM -> Triple(EmeraldContainer, EmeraldDark, Icons.Default.Groups)
                        NoteType.PRIVATE -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), Icons.Default.Lock)
                        NoteType.PAYMENT_REMINDER -> Triple(PendingAmberBg, PendingAmber, Icons.Default.AccessTime)
                        NoteType.DUE_DATE -> Triple(if (isOverdue) OverdueRedBg else Color(0xFFE0F2FE), if (isOverdue) OverdueRed else Color(0xFF0369A1), Icons.Default.Event)
                        NoteType.ADMIN_ANNOUNCEMENT -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.Campaign)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = VwsSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = if (isOverdue) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OverdueRed)) else CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_item_${note.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = badgeBg,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(badgeIcon, contentDescription = null, tint = badgeText, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = note.noteType.displayName,
                                            fontSize = 11.sp,
                                            color = badgeText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (note.dueDateMillis != null) {
                                    Surface(
                                        color = if (isOverdue) OverdueRedBg else VwsSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${if (isOverdue) "Overdue: " else "Due: "}${dueDateFormatter.format(Date(note.dueDateMillis))}",
                                            fontSize = 11.sp,
                                            color = if (isOverdue) OverdueRed else TextSecondary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = note.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.content,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = VwsBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "By ${if (isAuthor) "You" else author?.fullName ?: "Admin"}",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = dateFormat.format(Date(note.createdAtMillis)),
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
