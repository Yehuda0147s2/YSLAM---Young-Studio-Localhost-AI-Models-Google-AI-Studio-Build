package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.BackendType
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
import com.example.ui.components.permission.PermissionsCard
import com.example.viewmodel.YslamViewModel

@Composable
fun SettingsScreen(
    viewModel: YslamViewModel,
    onNavigateToApiKeys: () -> Unit,
    onOpenAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBackend by viewModel.activeBackend.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val localEndpoint by viewModel.localEndpoint.collectAsState()
    val healthStatus by viewModel.healthStatus.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()
    val currentUser by viewModel.authManager.currentUser.collectAsState()

    var endpointInput by remember(localEndpoint) { mutableStateOf(localEndpoint) }
    var modelInput by remember(activeModel) { mutableStateOf(activeModel) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YslamBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Developer Account / Auth Section
        Text(
            text = "DEVELOPER PROFILE & AUTHENTICATION",
            style = MaterialTheme.typography.labelSmall,
            color = YslamPurple,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YslamSurface)
                .border(1.dp, YslamBorder, RoundedCornerShape(12.dp))
                .clickable { onOpenAuth() }
                .padding(14.dp)
                .testTag("user_auth_settings_card"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (currentUser != null) YslamPurple else YslamSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser != null) {
                        Text(
                            text = (currentUser!!.displayName?.take(1) ?: "U").uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = YslamBackground
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = YslamTextMuted)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = currentUser?.displayName ?: "Sign in with Google",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = YslamTextPrimary
                    )
                    Text(
                        text = if (currentUser != null) "${currentUser!!.email} • Cloud Sync Enabled" else "Tap to authenticate with Firebase Auth",
                        fontSize = 11.sp,
                        color = if (currentUser != null) YslamGreen else YslamTextMuted
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = YslamTextSecondary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Key Management Section
        Text(
            text = "CREDENTIALS & SECURITY",
            style = MaterialTheme.typography.labelSmall,
            color = YslamAmber,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YslamSurface)
                .border(1.dp, YslamBorder, RoundedCornerShape(12.dp))
                .clickable { onNavigateToApiKeys() }
                .padding(14.dp)
                .testTag("api_keys_settings_card"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(YslamAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = YslamAmber, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "API Key Management",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = YslamTextPrimary
                    )
                    Text(
                        text = "Hardware encrypted AES-GCM keys for Gemini & Local",
                        fontSize = 11.sp,
                        color = YslamTextMuted
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = YslamTextSecondary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "INFERENCE ENGINE & BACKEND",
            style = MaterialTheme.typography.labelSmall,
            color = YslamCyan,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Backend Selection Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Gemini Cloud Card
            val isGemini = activeBackend == BackendType.GEMINI
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGemini) YslamSurfaceVariant else YslamSurface)
                    .border(
                        1.dp,
                        if (isGemini) YslamCyan else YslamBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { viewModel.setBackend(BackendType.GEMINI) }
                    .padding(14.dp)
                    .testTag("backend_card_gemini")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (isGemini) YslamCyan else YslamTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isGemini) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(YslamCyan)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Google Gemini",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isGemini) YslamCyan else YslamTextPrimary
                    )
                    Text(
                        text = "Cloud inference via Firebase AI Logic",
                        fontSize = 11.sp,
                        color = YslamTextMuted
                    )
                }
            }

            // Localhost Card
            val isLocal = activeBackend == BackendType.LOCAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLocal) YslamSurfaceVariant else YslamSurface)
                    .border(
                        1.dp,
                        if (isLocal) YslamAmber else YslamBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { viewModel.setBackend(BackendType.LOCAL) }
                    .padding(14.dp)
                    .testTag("backend_card_local")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = if (isLocal) YslamAmber else YslamTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isLocal) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(YslamAmber)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Localhost LLM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isLocal) YslamAmber else YslamTextPrimary
                    )
                    Text(
                        text = "Ollama / llama.cpp on local network",
                        fontSize = 11.sp,
                        color = YslamTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Backend specific configuration
        if (activeBackend == BackendType.GEMINI) {
            Text(
                text = "GEMINI CLOUD MODEL SELECTION",
                style = MaterialTheme.typography.labelSmall,
                color = YslamCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val geminiModels = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")
            geminiModels.forEach { model ->
                val isSelected = activeModel == model
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) YslamSurfaceVariant else YslamSurface)
                        .border(1.dp, if (isSelected) YslamCyan else YslamBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.setModel(model) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = model,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) YslamCyan else YslamTextPrimary
                        )
                        Text(
                            text = if (model.contains("3.8")) "Recommended general purpose coding model" else "Ultra-fast lightweight workhorse",
                            fontSize = 11.sp,
                            color = YslamTextMuted
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = YslamCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            // Localhost Settings
            Text(
                text = "LOCAL INFERENCE ENDPOINT & HOST",
                style = MaterialTheme.typography.labelSmall,
                color = YslamAmber,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = endpointInput,
                onValueChange = {
                    endpointInput = it
                    viewModel.setLocalEndpoint(it)
                },
                label = { Text("Ollama Server URL", color = YslamTextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("local_endpoint_input"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMono,
                    color = YslamTextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YslamAmber,
                    unfocusedBorderColor = YslamBorder,
                    focusedContainerColor = YslamSurface,
                    unfocusedContainerColor = YslamSurface
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Presets row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets = listOf(
                    "http://10.0.2.2:11434" to "Host (10.0.2.2)",
                    "http://192.168.1.100:11434" to "LAN IP",
                    "http://localhost:11434" to "Localhost"
                )
                presets.forEach { (url, label) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(YslamSurfaceVariant)
                            .border(1.dp, YslamBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                endpointInput = url
                                viewModel.setLocalEndpoint(url)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = YslamTextSecondary
                        )
                    }
                }
            }

            if (endpointInput.contains("localhost") || endpointInput.contains("127.0.0.1")) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(YslamSurfaceVariant)
                        .border(1.dp, YslamAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable {
                            val resolved = endpointInput
                                .replace("localhost", "10.0.2.2")
                                .replace("127.0.0.1", "10.0.2.2")
                            endpointInput = resolved
                            viewModel.setLocalEndpoint(resolved)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = YslamAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emulator Tip: Use 10.0.2.2 to reach host PC Ollama. Tap to switch.",
                        fontSize = 11.sp,
                        color = YslamAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = modelInput,
                onValueChange = {
                    modelInput = it
                    viewModel.setModel(it)
                },
                label = { Text("Model Tag (e.g. llama3, mistral, codellama)", color = YslamTextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("local_model_input"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMono,
                    color = YslamTextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YslamAmber,
                    unfocusedBorderColor = YslamBorder,
                    focusedContainerColor = YslamSurface,
                    unfocusedContainerColor = YslamSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Health Check / Connection Test Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YslamSurface)
                .border(1.dp, YslamBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONNECTION DIAGNOSTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = YslamTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { viewModel.checkBackendHealth() },
                    enabled = !isPinging,
                    colors = ButtonDefaults.buttonColors(containerColor = YslamSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, YslamBorder),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("ping_test_button")
                ) {
                    if (isPinging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = YslamCyan
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = YslamCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Connection", color = YslamCyan, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (healthStatus != null) {
                val status = healthStatus!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (status.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (status.isHealthy) YslamGreen else YslamRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (status.isHealthy) "Online (${status.latencyMs}ms)" else "Offline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (status.isHealthy) YslamGreen else YslamRed
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = status.message,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMono,
                            color = YslamTextSecondary
                        )

                        if (status.availableModels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Discovered models: ${status.availableModels.joinToString(", ")}",
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMono,
                                color = YslamCyan
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Tap 'Test Connection' to verify server reachability and latency.",
                    color = YslamTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Android Permissions & System Integration Dashboard
        PermissionsCard(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(20.dp))

        // Post-Export Architecture Callout Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CodeBackground)
                .border(1.dp, YslamCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = YslamCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "YSLAM ARCHITECTURAL SPECIFICATION",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = YslamCyan
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• Dual Backend: AiBackend interface shared by GeminiBackend (Firebase AI Logic) and LocalBackend (Ollama/llama.cpp HTTP stream).\n" +
                        "• Fixed Contract: The UI interacts strictly with AiBackend. When exporting this codebase, you can modify LocalBackend implementation without touching any Jetpack Compose UI code.\n" +
                        "• Android Emulator loopback is pre-configured to 10.0.2.2:11434 so host Ollama works immediately.",
                fontFamily = Inter,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = YslamTextSecondary
            )
        }
    }
}
