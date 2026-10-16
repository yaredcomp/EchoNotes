package com.example.ui.detail

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import com.example.ui.settings.ApiKeySettingsDialog
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ActionItem
import com.example.domain.model.Note
import com.example.ui.theme.StitchBackground
import com.example.ui.theme.StitchCardOutline
import com.example.ui.theme.StitchCyan
import com.example.ui.theme.StitchCyanContainer
import com.example.ui.theme.StitchCyanText
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    viewModel: NoteDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isTranscriptExpanded by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    if (showApiKeyDialog) {
        ApiKeySettingsDialog(
            onDismissRequest = { showApiKeyDialog = false }
        )
    }

    val note = uiState.note

    androidx.activity.compose.BackHandler {
        com.example.data.audio.GlobalAudioPlayer.stopAndRelease()
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            com.example.data.audio.GlobalAudioPlayer.stopAndRelease()
        }
    }

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
                    Text(
                        "Note Detail View",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = StitchTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            com.example.data.audio.GlobalAudioPlayer.stopAndRelease()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StitchTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("detail_api_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Gemini API Key Settings",
                            tint = StitchCyan
                        )
                    }

                    IconButton(onClick = {
                        note?.let {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, it.title)
                                putExtra(Intent.EXTRA_TEXT, "${it.title}\n\nExecutive Summary:\n${it.summary}\n\nTranscript:\n${it.transcript}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share EchoNotes"))
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = StitchTextSecondary)
                    }

                    IconButton(onClick = { 
                        com.example.data.audio.GlobalAudioPlayer.stopAndRelease()
                        note?.let { onNavigateToEdit(it.id) } 
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = StitchTextSecondary)
                    }

                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.testTag("delete_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = StitchRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StitchBackground)
            )
        },
        bottomBar = {
            // Sticky "Chat with Note" Pill Button matching screen.png
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StitchBackground
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(StitchPurple, StitchPurpleGradientEnd)
                                )
                            )
                            .clickable { 
                                note?.let { 
                                    com.example.data.audio.GlobalAudioPlayer.stopAndRelease()
                                    onNavigateToChat(it.id) 
                                } 
                            }
                            .testTag("chat_with_note_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Chat with Note",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading || note == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StitchCyan)
            }
        } else {
            val dateFormat = remember { SimpleDateFormat("MMM d • m'm' s's'", Locale.getDefault()) }
            val formattedDate = remember(note.timestamp) { dateFormat.format(Date(note.timestamp)) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Badges row: "AI Voice Memo", "Oct 11 • 4m 18s", "Transcribed"
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StitchPurpleContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = StitchPurpleText, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Voice Memo", fontSize = 11.sp, color = StitchPurpleText, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StitchSurfaceVariant
                    ) {
                        Text(
                            text = formattedDate,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 11.sp,
                            color = StitchTextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StitchCyanContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = StitchCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Transcribed", fontSize = 11.sp, color = StitchCyan, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Title: "What is an LLM?" (Serif / Display font style)
                Text(
                    text = note.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = StitchTextPrimary,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Audio Player Card matching screen.png
                StitchAudioPlayerCard(
                    playerState = uiState.playerState,
                    durationSeconds = note.durationSeconds,
                    onTogglePlay = { viewModel.togglePlayPause() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Transcript Snippet Card with Expand chevron
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.People, contentDescription = null, tint = StitchTextSecondary, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Transcript Snippet", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { isTranscriptExpanded = !isTranscriptExpanded }
                            ) {
                                Text("Expand", fontSize = 12.sp, color = StitchTextSecondary)
                                Icon(
                                    imageVector = if (isTranscriptExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = StitchTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val displayTranscript = if (isTranscriptExpanded) {
                            note.transcript.ifBlank { "No transcript text available." }
                        } else {
                            (note.transcript.take(120) + if (note.transcript.length > 120) "..." else "").ifBlank { "Voice note transcript recorded." }
                        }

                        Text(
                            text = displayTranscript,
                            fontSize = 13.sp,
                            color = StitchTextSecondary,
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. AI Executive Summary Card matching screen.png
                StitchExecutiveSummaryCard(
                    summary = note.summary,
                    isRegenerating = uiState.isRegeneratingAi,
                    onRegenerate = { viewModel.regenerateAiAnalysis() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Key Takeaways Section matching screen.png (with points counter)
                StitchKeyTakeawaysCard(takeaways = note.keyTakeaways)

                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        if (showDeleteConfirmation) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = {
                    Text(
                        text = "Move to Trash?",
                        fontWeight = FontWeight.Bold,
                        color = StitchTextPrimary,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "This recording will be moved to Trash. You can restore it anytime in Settings.",
                        color = StitchTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            showDeleteConfirmation = false
                            com.example.data.audio.GlobalAudioPlayer.stopAndRelease()
                            viewModel.deleteNote { onNavigateBack() }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = StitchRed)
                    ) {
                        Text("Move to Trash", color = Color.White)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showDeleteConfirmation = false }
                    ) {
                        Text("Cancel", color = StitchTextSecondary)
                    }
                },
                containerColor = StitchSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun StitchAudioPlayerCard(
    playerState: com.example.data.audio.AudioPlayerState,
    durationSeconds: Long,
    onTogglePlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(StitchPurple)
                    .clickable(onClick = onTogglePlay)
                    .testTag("play_pause_audio_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play Audio",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            val currentSec = playerState.currentPositionMs / 1000
            val totalSec = if (playerState.durationMs > 0) playerState.durationMs / 1000 else durationSeconds

            // Elapsed time
            Text(
                text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = StitchTextPrimary,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Waveform equalizer bars
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val heights = listOf(8.dp, 16.dp, 22.dp, 12.dp, 20.dp, 14.dp, 10.dp, 18.dp, 14.dp, 8.dp, 16.dp, 10.dp)
                heights.forEachIndexed { idx, h ->
                    val color = if (idx < 6) StitchTextPrimary else StitchTextMuted
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(h)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Total duration
            Text(
                text = String.format("%02d:%02d", totalSec / 60, totalSec % 60),
                fontSize = 12.sp,
                color = StitchTextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Speed pill: "1.0x"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StitchSurfaceVariant
            ) {
                Text(
                    text = "1.0x",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    color = StitchTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StitchExecutiveSummaryCard(
    summary: String,
    isRegenerating: Boolean,
    onRegenerate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchPurple.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchPurpleContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = StitchPurpleText, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AI Executive Summary",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchTextPrimary
                    )
                }

                if (isRegenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = StitchCyan)
                } else {
                    IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate", tint = StitchTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body text
            com.example.ui.components.RichMarkdownText(
                markdown = summary.ifBlank { "Executive summary will be synthesized once recording is processed." },
                fontSize = 14.sp,
                color = StitchTextPrimary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-tags inside card: "⚡ 98% confidence", "✦ Token Synthesis"
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchSurfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null, tint = StitchCyan, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("98% confidence", fontSize = 11.sp, color = StitchCyanText, fontWeight = FontWeight.Medium)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchSurfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = StitchPurpleText, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Token Synthesis", fontSize = 11.sp, color = StitchPurpleText, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun StitchKeyTakeawaysCard(takeaways: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = StitchCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Key Takeaways", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
            }
            Text("${takeaways.size} points", fontSize = 12.sp, color = StitchTextMuted)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                takeaways.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp, end = 10.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(StitchPurpleText)
                        )
                        Text(
                            text = com.example.ui.components.parseMarkdownInline(point, StitchTextSecondary),
                            fontSize = 13.sp,
                            color = StitchTextSecondary,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}


