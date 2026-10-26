package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.permission.VoiceDictationButton
import com.example.ui.theme.Inter
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.YslamAmber
import com.example.ui.theme.YslamBackground
import com.example.ui.theme.YslamBorder
import com.example.ui.theme.YslamCyan
import com.example.ui.theme.YslamGreen
import com.example.ui.theme.YslamPurple
import com.example.ui.theme.YslamRed
import com.example.ui.theme.YslamSurface
import com.example.ui.theme.YslamSurfaceVariant
import com.example.ui.theme.YslamTextMuted
import com.example.ui.theme.YslamTextPrimary
import com.example.ui.theme.YslamTextSecondary
import com.example.viewmodel.YslamViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: YslamViewModel,
    onNavigateToPlaygroundWithCode: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGeneratingChat.collectAsState()
    val streamingText by viewModel.chatStreamingText.collectAsState()
    val chatError by viewModel.chatError.collectAsState()
    val systemInstruction by viewModel.systemInstruction.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSessionsMenu by remember { mutableStateOf(false) }
    var showSystemPromptConfig by remember { mutableStateOf(false) }
    var editedSystemPrompt by remember { mutableStateOf(systemInstruction) }

    val listState = rememberLazyListState()

    // Auto-scroll on new message or streaming delta
    LaunchedEffect(messages.size, streamingText.length) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            val totalItems = messages.size + (if (streamingText.isNotEmpty()) 1 else 0)
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    val currentSession = sessions.find { it.id == currentSessionId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YslamBackground)
    ) {
        // Sessions & Thread Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(YslamSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showSessionsMenu = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentSession?.title ?: "New Chat",
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = YslamTextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Switch Chat",
                        tint = YslamTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSessionsMenu,
                    onDismissRequest = { showSessionsMenu = false },
                    modifier = Modifier
                        .background(YslamSurface)
                        .border(1.dp, YslamBorder, RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = YslamCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Chat Thread", color = YslamCyan, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        onClick = {
                            viewModel.createNewSession("New Chat")
                            showSessionsMenu = false
                        }
                    )

                    sessions.forEach { sess ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sess.title,
                                            color = if (sess.id == currentSessionId) YslamCyan else YslamTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (sess.id == currentSessionId) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = "${sess.backendType} • ${sess.modelName}",
                                            color = YslamTextMuted,
                                            fontSize = 10.sp,
                                            fontFamily = JetBrainsMono
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteSession(sess.id)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Thread",
                                            tint = YslamTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.selectSession(sess.id)
                                showSessionsMenu = false
                            }
                        )
                    }
                }
            }

            // Controls on the right
            Row(verticalAlignment = Alignment.CenterVertically) {
                // System Prompt button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showSystemPromptConfig) YslamCyan.copy(alpha = 0.2f) else YslamSurfaceVariant)
                        .border(
                            1.dp,
                            if (showSystemPromptConfig) YslamCyan else YslamBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            editedSystemPrompt = systemInstruction
                            showSystemPromptConfig = !showSystemPromptConfig
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "System Instructions",
                            tint = if (showSystemPromptConfig) YslamCyan else YslamTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "System",
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMono,
                            color = if (showSystemPromptConfig) YslamCyan else YslamTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.createNewSession("New Chat") },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(YslamSurfaceVariant)
                        .testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Thread",
                        tint = YslamCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expandable System Prompt Drawer
        AnimatedVisibility(visible = showSystemPromptConfig) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YslamSurfaceVariant)
                    .border(1.dp, YslamBorder)
                    .padding(12.dp)
            ) {
                Text(
                    text = "SYSTEM INSTRUCTION (APPLIED TO CHAT TURNS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = YslamCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = editedSystemPrompt,
                    onValueChange = { editedSystemPrompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 120.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMono,
                        color = YslamTextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YslamCyan,
                        unfocusedBorderColor = YslamBorder,
                        focusedContainerColor = YslamBackground,
                        unfocusedContainerColor = YslamBackground
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(YslamCyan)
                            .clickable {
                                viewModel.updateSystemInstruction(editedSystemPrompt)
                                showSystemPromptConfig = false
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Save Instruction",
                            color = YslamBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Messages Stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && streamingText.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(YslamSurfaceVariant)
                            .border(1.dp, YslamCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "</>",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = YslamCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "YSLAM Code Workspace",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = YslamTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask questions, design algorithms, refactor code, or test local models.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = YslamTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    // Preset quick prompt chips
                    val suggestions = listOf(
                        "Write an asynchronous HTTP server in Kotlin",
                        "Explain difference between Mutex and Semaphore",
                        "Create a Compose custom Canvas circular gauge"
                    )
                    suggestions.forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(YslamSurface)
                                .border(1.dp, YslamBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    inputText = suggestion
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMono,
                                color = YslamTextSecondary
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageItem(
                            message = message,
                            onSendToPlayground = onNavigateToPlaygroundWithCode
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Active streaming token card
                    if (streamingText.isNotEmpty()) {
                        item(key = "active_stream") {
                            ActiveStreamingItem(
                                streamingText = streamingText,
                                modelUsed = viewModel.activeModel.value,
                                onSendToPlayground = onNavigateToPlaygroundWithCode
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // Error Toast / Notification
            if (chatError != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(YslamRed.copy(alpha = 0.95f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = chatError ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Bottom Input Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(YslamSurface)
                .border(1.dp, YslamBorder)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask anything or paste code...",
                            color = YslamTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 130.dp)
                        .testTag("chat_input_field"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        color = YslamTextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YslamCyan,
                        unfocusedBorderColor = YslamBorder,
                        focusedContainerColor = YslamBackground,
                        unfocusedContainerColor = YslamBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Voice Dictation Button with runtime permission flow
                VoiceDictationButton(
                    onTextAppended = { speechText ->
                        inputText = if (inputText.isBlank()) speechText else "$inputText $speechText"
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (isGenerating) {
                    IconButton(
                        onClick = { viewModel.stopChatGeneration() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(YslamRed)
                            .testTag("stop_generation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Generation",
                            tint = Color.White
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val textToSend = inputText
                                inputText = ""
                                viewModel.sendChatMessage(textToSend)
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) YslamCyan else YslamSurfaceVariant)
                            .testTag("send_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (inputText.isNotBlank()) YslamBackground else YslamTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onSendToPlayground: (String, String) -> Unit
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_message_${message.id}"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Meta header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            Text(
                text = if (isUser) "YOU" else "YSLAM (${message.modelUsed ?: "AI"})",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isUser) YslamPurple else YslamCyan
            )
            if (!isUser && message.latencyMs != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${message.latencyMs}ms",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = YslamTextMuted
                )
            }
        }

        // Bubble Content
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.88f else 1.0f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isUser) YslamSurfaceVariant else YslamSurface)
                .border(
                    1.dp,
                    if (isUser) YslamPurple.copy(alpha = 0.4f) else YslamBorder,
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            MarkdownMessageView(
                content = message.content,
                isStreaming = false,
                onSendToPlayground = onSendToPlayground
            )
        }
    }
}

@Composable
fun ActiveStreamingItem(
    streamingText: String,
    modelUsed: String,
    onSendToPlayground: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streaming_chat_item"),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.5.dp,
                color = YslamCyan
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "STREAMING FROM $modelUsed...",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = YslamCyan
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YslamSurface)
                .border(1.dp, YslamCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            MarkdownMessageView(
                content = streamingText,
                isStreaming = true,
                onSendToPlayground = onSendToPlayground
            )
        }
    }
}
