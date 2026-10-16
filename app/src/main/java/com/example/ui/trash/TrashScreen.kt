package com.example.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Note
import com.example.ui.theme.StitchBackground
import com.example.ui.theme.StitchCardOutline
import com.example.ui.theme.StitchCyan
import com.example.ui.theme.StitchCyanContainer
import com.example.ui.theme.StitchCyanText
import com.example.ui.theme.StitchPurple
import com.example.ui.theme.StitchPurpleContainer
import com.example.ui.theme.StitchPurpleText
import com.example.ui.theme.StitchRed
import com.example.ui.theme.StitchRedContainer
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceVariant
import com.example.ui.theme.StitchTextMuted
import com.example.ui.theme.StitchTextPrimary
import com.example.ui.theme.StitchTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var noteToDeletePermanently by remember { mutableStateOf<Note?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = StitchBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Trash Collection",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = StitchTextPrimary
                        )
                        if (uiState.deletedNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = StitchRedContainer
                            ) {
                                Text(
                                    text = "${uiState.deletedNotes.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("trash_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StitchTextPrimary
                        )
                    }
                },
                actions = {
                    if (uiState.deletedNotes.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyTrashDialog = true },
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Text(
                                text = "Empty Trash",
                                color = StitchRed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StitchBackground)
            )
        }
    ) { padding ->
        if (uiState.deletedNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(StitchSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = StitchTextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Trash is Empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StitchTextPrimary,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Recordings you delete will appear here. You can restore them or delete them permanently.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = "Items in trash can be restored back to your notes library or permanently erased.",
                        fontSize = 12.sp,
                        color = StitchTextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                items(uiState.deletedNotes, key = { it.id }) { note ->
                    StitchTrashNoteCard(
                        note = note,
                        onRestore = { viewModel.restoreNote(note.id) },
                        onDeletePermanently = { noteToDeletePermanently = note }
                    )
                }
            }
        }

        // Delete Permanently Dialog
        if (noteToDeletePermanently != null) {
            AlertDialog(
                onDismissRequest = { noteToDeletePermanently = null },
                containerColor = StitchSurface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Delete Permanently?", color = StitchTextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "This will permanently erase \"${noteToDeletePermanently?.title}\", including its audio recording, transcript, and AI summary. This action cannot be undone.",
                        color = StitchTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            noteToDeletePermanently?.let { viewModel.deletePermanently(it.id) }
                            noteToDeletePermanently = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchRed)
                    ) {
                        Text("Delete Forever", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToDeletePermanently = null }) {
                        Text("Cancel", color = StitchTextSecondary)
                    }
                }
            )
        }

        // Empty Entire Trash Dialog
        if (showEmptyTrashDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyTrashDialog = false },
                containerColor = StitchSurface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Empty Trash?", color = StitchTextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Permanently delete all ${uiState.deletedNotes.size} recordings in the Trash Collection? This action cannot be reversed.",
                        color = StitchTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEmptyTrashDialog = false
                            viewModel.emptyTrash()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchRed)
                    ) {
                        Text("Empty Trash", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashDialog = false }) {
                        Text("Cancel", color = StitchTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun StitchTrashNoteCard(
    note: Note,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(note.timestamp) { dateFormat.format(Date(note.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trash_note_card_${note.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StitchCyanContainer
                ) {
                    Text(
                        text = note.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchCyanText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = note.formattedDuration,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchTextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = note.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StitchTextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Date
            Text(
                text = "Recorded: $formattedDate",
                fontSize = 11.sp,
                color = StitchTextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Snippet preview
            val snippet = (note.summary.ifBlank { note.transcript }).take(140)
            if (snippet.isNotBlank()) {
                Text(
                    text = if (snippet.length >= 140) "$snippet..." else snippet,
                    fontSize = 12.sp,
                    color = StitchTextSecondary,
                    lineHeight = 17.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Action Buttons: Restore & Delete Permanently
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StitchCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StitchCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("restore_note_button_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = StitchCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Restore",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchCyan
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onDeletePermanently,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchRedContainer),
                    modifier = Modifier.testTag("delete_permanently_button_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = StitchRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete Forever",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchRed
                    )
                }
            }
        }
    }
}
