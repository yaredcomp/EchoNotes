package com.example.ui.recorder

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.audio.GlobalAudioPlayer
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
import com.example.ui.theme.StitchRedContainer
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceVariant
import com.example.ui.theme.StitchTextMuted
import com.example.ui.theme.StitchTextPrimary
import com.example.ui.theme.StitchTextSecondary

@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    LaunchedEffect(uiState.completedNoteId) {
        uiState.completedNoteId?.let { noteId ->
            onNavigateToDetail(noteId)
        }
    }

    DisposableEffect(Unit) {
        // Enforce total silence from previous notes / players
        GlobalAudioPlayer.stopAndRelease()
        onDispose {
            if (uiState.completedNoteId == null) {
                GlobalAudioPlayer.stopAndRelease()
            }
            if (uiState.state == RecordingState.RECORDING || uiState.state == RecordingState.PAUSED) {
                viewModel.cancelRecording()
            }
        }
    }

    BackHandler {
        GlobalAudioPlayer.stopAndRelease()
        viewModel.cancelRecording()
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        GlobalAudioPlayer.stopAndRelease()
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (uiState.state == RecordingState.IDLE) {
            viewModel.startRecording()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Close X, "LIVE RECORDING Session #408 • Stereo", "● 48kHz HD" pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        GlobalAudioPlayer.stopAndRelease()
                        viewModel.cancelRecording()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StitchSurface)
                        .testTag("recorder_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = StitchTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LIVE  RECORDING",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StitchTextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Session #408 • Stereo",
                        fontSize = 12.sp,
                        color = StitchTextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StitchSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StitchGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "48kHz HD",
                            fontSize = 11.sp,
                            color = StitchTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!hasMicPermission && uiState.state == RecordingState.IDLE) {
                Spacer(modifier = Modifier.height(40.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPurple)
                    ) {
                        Text("Grant Microphone Access")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            hasMicPermission = true
                            viewModel.startRecording()
                        }
                    ) {
                        Text("Continue Recording", color = StitchCyan)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                // Transcription Mode Selector (Google Stitch segmented pill)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(StitchSurface)
                        .border(1.dp, StitchCardOutline, RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLive = uiState.transcriptionMode == TranscriptionMode.LIVE_STREAMING
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isLive) StitchPurple else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.setTranscriptionMode(TranscriptionMode.LIVE_STREAMING)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "⚡ Live Stream",
                                fontSize = 11.sp,
                                fontWeight = if (isLive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLive) Color.White else StitchTextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (!isLive) StitchCyanContainer else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.setTranscriptionMode(TranscriptionMode.ACCURATE_STT)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🎯 Accurate STT",
                                fontSize = 11.sp,
                                fontWeight = if (!isLive) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isLive) StitchCyan else StitchTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category selector pills
                val categories = listOf("Meetings", "Ideas", "Tasks", "Personal")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(categories) { cat ->
                        val isSelected = uiState.selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) StitchPurple else StitchSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StitchPurple else StitchCardOutline
                            ),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { viewModel.setCategory(cat) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else StitchTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Giant Timer matching screen 3.png: "00:04.82" (white and cyan)
                val minutes = uiState.durationSeconds / 60
                val seconds = uiState.durationSeconds % 60
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = StitchTextPrimary)) {
                            append(String.format("%02d:%02d", minutes, seconds))
                        }
                        withStyle(SpanStyle(color = StitchCyan, fontSize = 28.sp)) {
                            append(".82")
                        }
                    },
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Status pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StitchRedContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StitchRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(StitchRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.transcriptionMode == TranscriptionMode.LIVE_STREAMING) {
                                "RECORDING & TRANSCRIBING"
                            } else {
                                "RECORDING HIGH-FIDELITY AUDIO"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchRed,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dynamic Stitch Neon Waveform with dB indicators
                StitchWaveformCanvas(
                    amplitudes = uiState.amplitudes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                )

                // dB Labels Row below Waveform
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-48 dB", fontSize = 11.sp, color = StitchTextMuted)
                    Text("PEAK: -6.2 dB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchCyan)
                    Text("0 dB", fontSize = 11.sp, color = StitchTextMuted)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Middle Content Card: Live Stream vs Accurate STT
                if (uiState.transcriptionMode == TranscriptionMode.LIVE_STREAMING) {
                    // Live Speech-to-Text Stream Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            // Header: Sparkle, "Live Speech-to-Text Stream", "EN-US", "Real Mic"
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
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = StitchPurpleText,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Live Speech Stream",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchTextPrimary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StitchSurfaceVariant
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            fontSize = 10.sp,
                                            color = StitchCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "SPEAKER 1 (YOU)  00:01",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StitchTextMuted,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val scrollState = rememberScrollState()
                            LaunchedEffect(uiState.liveTranscript, uiState.partialTranscript) {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StitchSurfaceVariant.copy(alpha = 0.6f))
                                    .padding(12.dp)
                                    .verticalScroll(scrollState)
                            ) {
                                Column {
                                    if (uiState.liveTranscript.isNotBlank()) {
                                        Text(
                                            text = uiState.liveTranscript,
                                            fontSize = 14.sp,
                                            color = StitchTextPrimary,
                                            lineHeight = 20.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(18.dp)
                                                .background(StitchCyan)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (uiState.partialTranscript.isNotBlank())
                                                uiState.partialTranscript
                                            else if (uiState.liveTranscript.isBlank())
                                                "Listening... Speak naturally into microphone."
                                            else
                                                "Listening...",
                                            fontSize = 13.sp,
                                            color = if (uiState.partialTranscript.isNotBlank()) StitchCyan else StitchTextMuted,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val wordCount = (uiState.liveTranscript + " " + uiState.partialTranscript)
                                .split(Regex("\\s+"))
                                .filter { it.isNotBlank() }
                                .size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Only spoken words appear", fontSize = 11.sp, color = StitchTextMuted)
                                Text("$wordCount Words", fontSize = 11.sp, color = StitchCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Accurate Speech-to-Text Card (Focused on clean recording & post-transcription)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(StitchCyanContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = StitchCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Accurate Speech-to-Text Mode",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchTextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Capturing pristine audio without live streaming artifacts. Complete transcription and AI executive summary will generate with high accuracy when you tap Finish.",
                                fontSize = 12.sp,
                                color = StitchTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StitchSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StitchGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "High-Precision Pipeline Active",
                                        fontSize = 11.sp,
                                        color = StitchTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Controls Row: Discard (round), Pause (round glow), Finish (pill)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Discard
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                GlobalAudioPlayer.stopAndRelease()
                                viewModel.cancelRecording()
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(StitchSurface)
                                .border(1.dp, StitchCardOutline, CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Discard", tint = StitchTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Discard", fontSize = 11.sp, color = StitchTextMuted)
                    }

                    // Pause / Resume with glowing aura ring
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(StitchPurple.copy(alpha = 0.2f))
                                .border(2.dp, StitchPurple, CircleShape)
                                .clickable {
                                    if (uiState.state == RecordingState.RECORDING) viewModel.pauseRecording()
                                    else viewModel.resumeRecording()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(StitchPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.state == RecordingState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Toggle pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.state == RecordingState.RECORDING) "Pause" else "Resume",
                            fontSize = 11.sp,
                            color = StitchTextMuted
                        )
                    }

                    // Finish pill button (neon cyan gradient with check icon)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { viewModel.finishRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(StitchCyan, StitchPurple)
                                    )
                                )
                                .testTag("finish_recording_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Finish",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Save & AI", fontSize = 11.sp, color = StitchTextMuted)
                    }
                }
            }
        }

        // Processing overlay if synthesizing AI analysis
        if (uiState.state == RecordingState.PROCESSING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        color = StitchCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Voice Intelligence Synthesis",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.statusMessage,
                        fontSize = 13.sp,
                        color = StitchTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchWaveformCanvas(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val surfaceVariant = StitchSurfaceVariant

    Canvas(modifier = modifier) {
        val totalBars = 36
        val barWidth = 4.dp.toPx()
        val spacing = (size.width - (totalBars * barWidth)) / (totalBars - 1).coerceAtLeast(1)
        val centerY = size.height / 2f

        for (i in 0 until totalBars) {
            val amp = if (amplitudes.isNotEmpty()) {
                val idx = (amplitudes.size - 1) - (totalBars - 1 - i)
                if (idx in amplitudes.indices) amplitudes[idx] else 0.15f
            } else {
                0.2f
            }

            val dynamicHeight = (amp * (size.height * 0.9f) * pulse).coerceIn(8f, size.height * 0.95f)
            val x = i * (barWidth + spacing)
            val top = centerY - dynamicHeight / 2f

            val isMiddleActive = i in 12..24
            val barBrush = if (isMiddleActive) {
                Brush.verticalGradient(
                    colors = listOf(StitchCyan, StitchPurple),
                    startY = top,
                    endY = top + dynamicHeight
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(StitchPurple.copy(alpha = 0.6f), surfaceVariant),
                    startY = top,
                    endY = top + dynamicHeight
                )
            }

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, top),
                size = Size(barWidth, dynamicHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
