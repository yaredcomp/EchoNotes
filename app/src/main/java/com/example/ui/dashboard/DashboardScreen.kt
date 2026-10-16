package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import com.example.ui.settings.ApiKeySettingsDialog
import com.example.ui.theme.ThemeManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Note
import com.example.ui.theme.StitchBackground
import com.example.ui.theme.StitchCardOutline
import com.example.ui.theme.StitchCyan
import com.example.ui.theme.StitchCyanContainer
import com.example.ui.theme.StitchCyanText
import com.example.ui.theme.StitchGreen
import com.example.ui.theme.StitchGreenContainer
import com.example.ui.theme.StitchPurple
import com.example.ui.theme.StitchPurpleContainer
import com.example.ui.theme.StitchPurpleGradientEnd
import com.example.ui.theme.StitchPurpleText
import com.example.ui.theme.StitchRed
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceVariant
import com.example.ui.theme.StitchTextMuted
import com.example.ui.theme.StitchTextPrimary
import com.example.ui.theme.StitchTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToRecord: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    if (showApiKeyDialog) {
        ApiKeySettingsDialog(
            onDismissRequest = { showApiKeyDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = StitchBackground,
        floatingActionButton = {
            // Glowing Gradient "Record Note" Pill Button matching Stitch
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp, end = 4.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(StitchPurple, StitchPurpleGradientEnd)
                        )
                    )
                    .clickable(onClick = onNavigateToRecord)
                    .testTag("record_note_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Note",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Record Note",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header matching screen 5.png
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sparkle Avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(StitchPurple, StitchPurpleGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "EchoNotes",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title: Echo (White) Notes (Cyan)
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = StitchTextPrimary)) {
                                append("Echo")
                            }
                            withStyle(SpanStyle(color = StitchCyan)) {
                                append("Notes")
                            }
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // "AI" Badge Pill
                    Surface(
                        shape = CircleShape,
                        color = StitchPurpleContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchPurpleText
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("dashboard_api_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Gemini API Key Settings",
                            tint = StitchCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { ThemeManager.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (ThemeManager.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (ThemeManager.isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = if (ThemeManager.isDarkTheme) StitchPurpleText else StitchPurple,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = StitchTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Background Transcription In-Progress Alert Banner
            val backgroundTasks by com.example.data.speech.BackgroundTranscriptionManager.activeTasks.collectAsState()
            val activeProcessingTask = backgroundTasks.values.firstOrNull { !it.isComplete }
            if (activeProcessingTask != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StitchCyanContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StitchCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onNavigateToDetail(activeProcessingTask.noteId) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = StitchCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Transcribing in background",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchCyan
                            )
                            Text(
                                text = "${activeProcessingTask.noteTitle} • Tap to view audio",
                                fontSize = 11.sp,
                                color = StitchTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Search Bar with Filter Slider Icon (matching screen 5.png)
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("search_input"),
                placeholder = {
                    Text(
                        "Search transcripts, notes, tags...",
                        color = StitchTextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = StitchTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = StitchTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StitchPurple,
                    unfocusedBorderColor = StitchCardOutline,
                    focusedContainerColor = StitchSurface,
                    unfocusedContainerColor = StitchSurface,
                    focusedTextColor = StitchTextPrimary,
                    unfocusedTextColor = StitchTextPrimary
                ),
                singleLine = true
            )

            // Category Filter Pills (matching screen 5.png)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.categories) { category ->
                    val isSelected = uiState.selectedCategory == category
                    val labelText = if (category == "All") "All ${uiState.notes.size}" else category

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) StitchPurple else StitchSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) StitchPurple else StitchCardOutline
                        ),
                        modifier = Modifier.clickable { viewModel.onCategorySelected(category) }
                    ) {
                        Text(
                            text = labelText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White else StitchTextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Notes List
            if (uiState.notes.isEmpty()) {
                EmptyStitchNotesView(
                    isSearch = uiState.searchQuery.isNotEmpty(),
                    onStartRecord = onNavigateToRecord
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp)
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        StitchNoteCard(
                            note = note,
                            onClick = { onNavigateToDetail(note.id) },
                            onDelete = { noteToDelete = note }
                        )
                    }
                }
            }
        }
    }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            containerColor = StitchSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Move to Trash?", color = StitchTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This recording will be moved to the Trash Collection. You can restore or delete it permanently from Settings.", color = StitchTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        noteToDelete?.let { viewModel.deleteNote(it.id) }
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchRed)
                ) {
                    Text("Move to Trash", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel", color = StitchTextSecondary)
                }
            }
        )
    }
}

@Composable
fun StitchNoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(note.timestamp) { dateFormat.format(Date(note.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title, Live indicator, Trash, More menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = StitchTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Green live pulse dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(StitchGreen)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = StitchTextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Timestamp and Duration Row with Play icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = StitchTextMuted
                )
                Text(
                    text = " • ",
                    fontSize = 12.sp,
                    color = StitchTextMuted
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = StitchPurpleText,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = note.formattedDuration,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchPurpleText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Snippet Text
            val snippet = if (note.summary.isNotBlank()) note.summary else note.transcript.ifBlank { "Voice note recorded session." }
            Text(
                text = snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = StitchTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Badges Row (matching screen 5.png)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Action Items Badge
                    if (note.actionItems.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StitchSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StitchCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${note.actionItems.size} Action items",
                                    fontSize = 11.sp,
                                    color = StitchTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // AI Summarized or Transcribed Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StitchPurpleContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, StitchPurple.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StitchPurpleText,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI Summarized",
                                fontSize = 11.sp,
                                color = StitchPurpleText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Mini soundwave icon on right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val heights = listOf(8.dp, 14.dp, 18.dp, 10.dp)
                    heights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(h)
                                .clip(RoundedCornerShape(2.dp))
                                .background(StitchPurpleText)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStitchNotesView(
    isSearch: Boolean,
    onStartRecord: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(StitchPurpleContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearch) Icons.Default.Search else Icons.Default.Mic,
                    contentDescription = null,
                    tint = StitchPurpleText,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isSearch) "No notes match your search" else "No voice notes captured yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StitchTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSearch) "Try searching for a different keyword or tag" else "Tap 'Record Note' to capture lectures, meetings, and ideas with live transcription and Gemini AI synthesis.",
                style = MaterialTheme.typography.bodyMedium,
                color = StitchTextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 13.sp
            )
        }
    }
}
