package com.example.ui.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiAiClient
import com.example.data.local.preferences.UserPreferencesManager
import com.example.ui.theme.StitchCardOutline
import com.example.ui.theme.StitchCyan
import com.example.ui.theme.StitchPurple
import com.example.ui.theme.StitchPurpleContainer
import com.example.ui.theme.StitchPurpleText
import com.example.ui.theme.StitchRed
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceVariant
import com.example.ui.theme.StitchTextMuted
import com.example.ui.theme.StitchTextPrimary
import com.example.ui.theme.StitchTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ApiKeySettingsDialog(
    onDismissRequest: () -> Unit,
    onKeySaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { UserPreferencesManager.getInstance(context) }
    val geminiClient = remember { GeminiAiClient(context) }

    val initialSavedKey = remember { prefsManager.getCustomGeminiApiKey() }
    val initialActiveDescription = remember { geminiClient.getActiveKeySourceDescription() }

    var apiKeyInput by remember { mutableStateOf(initialSavedKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultSuccess by remember { mutableStateOf<Boolean?>(null) }
    var testResultMessage by remember { mutableStateOf<String?>(null) }

    var activeSourceDesc by remember { mutableStateOf(initialActiveDescription) }
    var activeModelName by remember { mutableStateOf(geminiClient.getActiveModel()) }
    var selectedModelOption by remember {
        mutableStateOf(if (activeModelName.isNotBlank() && activeModelName != "auto") activeModelName else "Auto")
    }

    fun testKey(keyCandidate: String, modelPref: String? = null) {
        if (keyCandidate.isBlank()) {
            testResultSuccess = false
            testResultMessage = "Please enter an API key to test."
            return
        }
        isTestingConnection = true
        testResultSuccess = null
        testResultMessage = null

        val prefToTest = if (modelPref != null && modelPref != "Auto") modelPref else if (selectedModelOption != "Auto") selectedModelOption else null

        coroutineScope.launch {
            val result = geminiClient.testConnection(keyCandidate, modelPreference = prefToTest)
            isTestingConnection = false
            result.onSuccess { msg ->
                testResultSuccess = true
                testResultMessage = msg
                activeModelName = geminiClient.getActiveModel()
                activeSourceDesc = geminiClient.getActiveKeySourceDescription()
            }.onFailure { err ->
                testResultSuccess = false
                testResultMessage = err.message ?: "Connection test failed"
                activeModelName = geminiClient.getActiveModel()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("api_key_settings_dialog"),
        containerColor = StitchSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StitchPurpleContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = StitchPurpleText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Gemini API Configuration",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = StitchTextPrimary
                    )
                    Text(
                        text = "Powers Summary, Takeaways & Note Chat",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = StitchTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Active Source Status Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchSurfaceVariant.copy(alpha = 0.6f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StitchCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Active Key Source:",
                                fontSize = 11.sp,
                                color = StitchTextMuted
                            )
                            Text(
                                text = activeSourceDesc,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Engine Model: $activeModelName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = StitchCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter or override your Gemini API key below. Changes are saved locally on this device.",
                    fontSize = 12.sp,
                    color = StitchTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        testResultSuccess = null
                        testResultMessage = null
                    },
                    label = { Text("Gemini API Key", fontSize = 12.sp) },
                    placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { testKey(apiKeyInput) }),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (apiKeyInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { apiKeyInput = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = StitchTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { isKeyVisible = !isKeyVisible },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isKeyVisible) "Hide key" else "Show key",
                                    tint = StitchTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchCyan,
                        unfocusedBorderColor = StitchCardOutline,
                        focusedTextColor = StitchTextPrimary,
                        unfocusedTextColor = StitchTextPrimary,
                        cursorColor = StitchCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipData = clipboard?.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString()?.trim() ?: ""
                                if (text.isNotBlank()) {
                                    apiKeyInput = text.trim('"', '\'')
                                }
                            }
                        },
                        modifier = Modifier.testTag("paste_api_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = StitchCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste from Clipboard", fontSize = 11.sp, color = StitchCyan)
                    }

                    if (prefsManager.hasCustomGeminiApiKey()) {
                        TextButton(
                            onClick = {
                                prefsManager.clearCustomGeminiApiKey()
                                apiKeyInput = ""
                                activeSourceDesc = geminiClient.getActiveKeySourceDescription()
                                activeModelName = geminiClient.getActiveModel()
                                testResultSuccess = null
                                testResultMessage = "Reverted to .env configuration."
                            },
                            modifier = Modifier.testTag("reset_to_env_button")
                        ) {
                            Text("Reset to .env", fontSize = 11.sp, color = StitchRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Model Preference (Auto-switches if overloaded):",
                    fontSize = 11.sp,
                    color = StitchTextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modelOptions = listOf("Auto", "gemini-1.5-flash", "gemini-2.0-flash")
                    modelOptions.forEach { opt ->
                        val isSelected = (opt == "Auto" && (selectedModelOption == "Auto" || selectedModelOption.isEmpty())) ||
                                (selectedModelOption.contains(opt, ignoreCase = true) && opt != "Auto")
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedModelOption = opt
                                testResultSuccess = null
                                testResultMessage = null
                            },
                            label = {
                                Text(
                                    text = if (opt == "Auto") "Auto (Recommended)" else opt.removePrefix("gemini-"),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StitchCyan.copy(alpha = 0.2f),
                                selectedLabelColor = StitchCyan,
                                containerColor = StitchSurfaceVariant,
                                labelColor = StitchTextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StitchCyan else StitchCardOutline
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test Connection Button
                OutlinedButton(
                    onClick = {
                        val keyToTest = if (apiKeyInput.isNotBlank()) apiKeyInput else geminiClient.getApiKey()
                        testKey(keyToTest, if (selectedModelOption != "Auto") selectedModelOption else null)
                    },
                    enabled = !isTestingConnection,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = StitchPurpleText
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StitchPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_connection_button")
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = StitchCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-detecting models & testing...", fontSize = 13.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Connection", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Test Result Feedback Banner
                AnimatedVisibility(
                    visible = testResultMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    testResultMessage?.let { msg ->
                        val isSuccess = testResultSuccess == true
                        val bg = if (isSuccess) Color(0xFF0F382A) else Color(0xFF381515)
                        val border = if (isSuccess) Color(0xFF26A69A) else StitchRed
                        val textColor = if (isSuccess) Color(0xFF80CBC4) else Color(0xFFFF8A80)

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = bg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = textColor,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = apiKeyInput.trim().trim('"', '\'')
                    if (clean.isNotBlank()) {
                        prefsManager.setCustomGeminiApiKey(clean)
                    } else {
                        prefsManager.clearCustomGeminiApiKey()
                    }
                    if (selectedModelOption != "Auto" && selectedModelOption.isNotBlank()) {
                        prefsManager.setActiveGeminiModel(selectedModelOption)
                    }
                    onKeySaved()
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_api_key_button")
            ) {
                Text("Save Key", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("cancel_api_key_button")
            ) {
                Text("Cancel", color = StitchTextSecondary)
            }
        }
    )
}
