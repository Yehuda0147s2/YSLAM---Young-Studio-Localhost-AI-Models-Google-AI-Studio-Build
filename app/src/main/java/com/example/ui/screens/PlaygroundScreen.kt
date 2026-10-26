package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import com.example.ui.components.CodeBlockView
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.permission.VoiceDictationButton
import com.example.ui.theme.CodeBackground
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

data class CodeTemplate(
    val title: String,
    val language: String,
    val prompt: String
)

val CODING_TEMPLATES = listOf(
    CodeTemplate(
        title = "LRU Cache (Kotlin)",
        language = "kotlin",
        prompt = "// Write an idiomatic, thread-safe LRU Cache in Kotlin with O(1) get and put operations."
    ),
    CodeTemplate(
        title = "Retrofit + Coroutines",
        language = "kotlin",
        prompt = "// Create a modern Android networking layer using Retrofit, OkHttp, and Kotlin Flow for handling GitHub API repositories."
    ),
    CodeTemplate(
        title = "Asyncio Task Pool (Python)",
        language = "python",
        prompt = "# Implement an asyncio concurrent worker queue in Python with rate limiting and exponential backoff retry."
    ),
    CodeTemplate(
        title = "Strict State Reducer (TS)",
        language = "typescript",
        prompt = "// Write a strictly typed state machine / reducer in TypeScript for an e-commerce checkout flow."
    ),
    CodeTemplate(
        title = "Complex SQL Window Function",
        language = "sql",
        prompt = "-- Write a query computing 7-day moving averages and customer retention cohort analysis in PostgreSQL."
    )
)

@Composable
fun PlaygroundScreen(
    viewModel: YslamViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.playgroundState.collectAsState()
    val snippets by viewModel.snippets.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()

    var showTemplatesMenu by remember { mutableStateOf(false) }
    var showSnippetsDialog by remember { mutableStateOf(false) }
    var showSaveTitleDialog by remember { mutableStateOf(false) }
    var showHyperparameters by remember { mutableStateOf(false) }
    var showSystemPrompt by remember { mutableStateOf(false) }

    val languages = listOf("kotlin", "python", "typescript", "rust", "sql", "bash")

    if (showSnippetsDialog) {
        SavedSnippetsDialog(
            snippets = snippets,
            onSelectSnippet = { snippet ->
                viewModel.loadPlaygroundTemplate(snippet.title, snippet.userPrompt, snippet.language)
            },
            onDeleteSnippet = { id -> viewModel.deleteSnippet(id) },
            onDismiss = { showSnippetsDialog = false }
        )
    }

    if (showSaveTitleDialog) {
        SaveSnippetTitleDialog(
            initialTitle = "${state.language.uppercase()} Snippet",
            onConfirm = { title ->
                viewModel.saveCurrentSnippet(title)
                showSaveTitleDialog = false
            },
            onDismiss = { showSaveTitleDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YslamBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("playground_screen")
    ) {
        // Top Toolbar: Template picker + Snippets Manager
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Template Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(YslamSurface)
                        .border(1.dp, YslamBorder, RoundedCornerShape(8.dp))
                        .clickable { showTemplatesMenu = true }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("templates_dropdown"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Templates",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = YslamCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Template",
                        tint = YslamCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showTemplatesMenu,
                    onDismissRequest = { showTemplatesMenu = false },
                    modifier = Modifier
                        .background(YslamSurface)
                        .border(1.dp, YslamBorder, RoundedCornerShape(8.dp))
                ) {
                    CODING_TEMPLATES.forEach { tpl ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = tpl.title,
                                        color = YslamTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = tpl.language.uppercase(),
                                        color = YslamCyan,
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMono
                                    )
                                }
                            },
                            onClick = {
                                viewModel.loadPlaygroundTemplate(tpl.title, tpl.prompt, tpl.language)
                                showTemplatesMenu = false
                            }
                        )
                    }
                }
            }

            // Snippets Button & Hyperparameters Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showSnippetsDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(YslamSurface)
                        .border(1.dp, YslamBorder, RoundedCornerShape(8.dp))
                        .testTag("open_snippets_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Saved Snippets",
                        tint = YslamPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showHyperparameters) YslamCyan.copy(alpha = 0.2f) else YslamSurface)
                        .border(
                            1.dp,
                            if (showHyperparameters) YslamCyan else YslamBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { showHyperparameters = !showHyperparameters }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("hyperparameters_toggle")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Parameters",
                            tint = if (showHyperparameters) YslamCyan else YslamTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "T=${"%.2f".format(state.temperature)}",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = if (showHyperparameters) YslamCyan else YslamTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Language Selector Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { lang ->
                val isSelected = state.language.equals(lang, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) YslamCyan else YslamSurface)
                        .border(
                            1.dp,
                            if (isSelected) YslamCyan else YslamBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.updatePlaygroundLanguage(lang) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("lang_chip_$lang")
                ) {
                    Text(
                        text = lang.uppercase(),
                        fontFamily = JetBrainsMono,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (isSelected) YslamBackground else YslamTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Collapsible Hyperparameter Drawer
        AnimatedVisibility(visible = showHyperparameters) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(YslamSurface)
                    .border(1.dp, YslamBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "HYPERPARAMETERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = YslamCyan,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Temperature Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Temperature (Creativity)", fontSize = 12.sp, color = YslamTextSecondary)
                    Text("${"%.2f".format(state.temperature)}", fontFamily = JetBrainsMono, fontSize = 12.sp, color = YslamCyan)
                }
                Slider(
                    value = state.temperature,
                    onValueChange = {
                        viewModel.updatePlaygroundParams(it, state.topP, state.maxTokens)
                    },
                    valueRange = 0.0f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = YslamCyan,
                        activeTrackColor = YslamCyan,
                        inactiveTrackColor = YslamBorder
                    )
                )

                // Top-P Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Top-P (Nucleus Sampling)", fontSize = 12.sp, color = YslamTextSecondary)
                    Text("${"%.2f".format(state.topP)}", fontFamily = JetBrainsMono, fontSize = 12.sp, color = YslamCyan)
                }
                Slider(
                    value = state.topP,
                    onValueChange = {
                        viewModel.updatePlaygroundParams(state.temperature, it, state.maxTokens)
                    },
                    valueRange = 0.05f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = YslamCyan,
                        activeTrackColor = YslamCyan,
                        inactiveTrackColor = YslamBorder
                    )
                )

                // Max Output Tokens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Max Output Tokens", fontSize = 12.sp, color = YslamTextSecondary)
                    Text("${state.maxTokens}", fontFamily = JetBrainsMono, fontSize = 12.sp, color = YslamCyan)
                }
                Slider(
                    value = state.maxTokens.toFloat(),
                    onValueChange = {
                        viewModel.updatePlaygroundParams(state.temperature, state.topP, it.toInt())
                    },
                    valueRange = 256f..4096f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = YslamCyan,
                        activeTrackColor = YslamCyan,
                        inactiveTrackColor = YslamBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // System Instruction Header & Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSystemPrompt = !showSystemPrompt }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM INSTRUCTION",
                style = MaterialTheme.typography.labelSmall,
                color = YslamPurple,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (showSystemPrompt) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = YslamPurple,
                modifier = Modifier.size(16.dp)
            )
        }

        if (showSystemPrompt) {
            OutlinedTextField(
                value = state.systemPrompt,
                onValueChange = { viewModel.updatePlaygroundSystemPrompt(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 110.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMono,
                    color = YslamTextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YslamPurple,
                    unfocusedBorderColor = YslamBorder,
                    focusedContainerColor = YslamSurface,
                    unfocusedContainerColor = YslamSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Code Prompt Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROMPT & CODE TASK",
                style = MaterialTheme.typography.labelSmall,
                color = YslamCyan,
                fontWeight = FontWeight.Bold
            )
            VoiceDictationButton(
                compact = true,
                onTextAppended = { speech ->
                    val current = state.userPrompt
                    val updated = if (current.isBlank()) speech else "$current\n$speech"
                    viewModel.updatePlaygroundPrompt(updated)
                }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = state.userPrompt,
            onValueChange = { viewModel.updatePlaygroundPrompt(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 220.dp)
                .testTag("playground_prompt_input"),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = JetBrainsMono,
                color = YslamTextPrimary,
                lineHeight = 20.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YslamCyan,
                unfocusedBorderColor = YslamBorder,
                focusedContainerColor = CodeBackground,
                unfocusedContainerColor = CodeBackground
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Action Run / Stop Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isGenerating) {
                    Button(
                        onClick = { viewModel.stopPlayground() },
                        colors = ButtonDefaults.buttonColors(containerColor = YslamRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("playground_stop_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.runPlaygroundPrompt() },
                        enabled = state.userPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YslamCyan,
                            disabledContainerColor = YslamSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("playground_run_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = YslamBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Run Code Prompt",
                            color = if (state.userPrompt.isNotBlank()) YslamBackground else YslamTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (state.generatedCode.isNotBlank()) {
                Button(
                    onClick = { showSaveTitleDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = YslamSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, YslamBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_snippet_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = YslamGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Snippet", color = YslamGreen, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Generation Output Area
        if (state.isGenerating || state.generatedCode.isNotBlank() || state.error != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(YslamSurface)
                    .border(1.dp, if (state.error != null) YslamRed else YslamBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                // Header with status & latency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = YslamCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GENERATING FROM $activeModel...",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = YslamCyan
                            )
                        } else {
                            Text(
                                text = "GENERATION OUTPUT",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = YslamGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (state.latencyMs != null) {
                        Text(
                            text = "${state.latencyMs}ms",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = YslamTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.error != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(YslamRed.copy(alpha = 0.2f))
                            .border(1.dp, YslamRed, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = YslamRed,
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMono
                        )
                    }
                } else {
                    MarkdownMessageView(
                        content = state.generatedCode,
                        isStreaming = state.isGenerating
                    )
                }
            }
        }
    }
}
