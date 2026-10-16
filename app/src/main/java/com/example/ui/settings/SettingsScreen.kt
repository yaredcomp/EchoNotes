package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StitchBackground
import com.example.ui.theme.StitchCardOutline
import com.example.ui.theme.StitchCyan
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
import com.example.ui.theme.ThemeManager

import androidx.compose.material.icons.filled.Key
import com.example.data.ai.GeminiAiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTrash: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLiveSttFallbackEnabled by remember { mutableStateOf(true) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val geminiClient = remember { GeminiAiClient(context) }
    var keySourceDescription by remember { mutableStateOf(geminiClient.getActiveKeySourceDescription()) }
    var activeModelName by remember { mutableStateOf(geminiClient.getActiveModel()) }

    if (showApiKeyDialog) {
        ApiKeySettingsDialog(
            onDismissRequest = {
                showApiKeyDialog = false
                keySourceDescription = geminiClient.getActiveKeySourceDescription()
                activeModelName = geminiClient.getActiveModel()
            },
            onKeySaved = {
                keySourceDescription = geminiClient.getActiveKeySourceDescription()
                activeModelName = geminiClient.getActiveModel()
            }
        )
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
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = StitchTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StitchTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StitchBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Appearance Section (Theme switch)
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = StitchCyan
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StitchPurpleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = StitchPurpleText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Dark Mode",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = StitchTextPrimary
                            )
                            Text(
                                if (ThemeManager.isDarkTheme) "Ultra-dark Google Stitch theme" else "Clean modern light theme",
                                fontSize = 12.sp,
                                color = StitchTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = ThemeManager.isDarkTheme,
                        onCheckedChange = { ThemeManager.setDarkTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StitchCyan,
                            checkedTrackColor = StitchPurple
                        ),
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trash & Data Management Section
            Text(
                text = "Trash & Recovery",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = StitchCyan
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onNavigateToTrash)
                    .testTag("trash_collection_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StitchRedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = StitchRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Trash Collection",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = StitchTextPrimary
                            )
                            Text(
                                "Restore deleted recordings or delete forever",
                                fontSize = 12.sp,
                                color = StitchTextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "View Trash",
                        tint = StitchTextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI Intelligence Section
            Text(
                text = "Voice & AI Intelligence",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = StitchCyan
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = StitchPurpleText)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Speech-to-Text Fallback", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = StitchTextPrimary)
                                Text("Auto-recovers audio stream if interrupted", fontSize = 12.sp, color = StitchTextSecondary)
                            }
                        }
                        Switch(
                            checked = isLiveSttFallbackEnabled,
                            onCheckedChange = { isLiveSttFallbackEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StitchCyan,
                                checkedTrackColor = StitchPurple
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = StitchCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI Model Engine", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = StitchTextPrimary)
                            Text("Google $activeModelName (Auto-Detected)", fontSize = 12.sp, color = StitchCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showApiKeyDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = StitchPurpleText)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Gemini API Key", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = StitchTextPrimary)
                                Text(keySourceDescription, fontSize = 12.sp, color = StitchTextSecondary)
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Configure Key",
                            tint = StitchTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Storage & Security Section
            Text(
                text = "Storage & Grounding",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = StitchCyan
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = StitchCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Grounded Intelligence", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = StitchTextPrimary)
                            Text("Chat & takeaways are strictly derived from your note transcripts", fontSize = 12.sp, color = StitchTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = StitchTextSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Local File System", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = StitchTextPrimary)
                            Text("Audio (.m4a), Transcripts (.txt), Summaries (.md)", fontSize = 12.sp, color = StitchTextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("EchoNotes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StitchCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("AI Voice Intelligence & Real-Time Synthesis", fontSize = 13.sp, color = StitchTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("v1.3.0 • Google Stitch Edition", fontSize = 11.sp, color = StitchTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
