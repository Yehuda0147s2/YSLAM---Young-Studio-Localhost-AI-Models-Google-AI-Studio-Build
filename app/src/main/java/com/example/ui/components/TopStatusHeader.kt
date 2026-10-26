package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.backend.BackendType
import com.example.backend.HealthStatus
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

@Composable
fun TopStatusHeader(
    activeBackend: BackendType,
    activeModel: String,
    healthStatus: HealthStatus?,
    userDisplayName: String?,
    onSwitchBackend: (BackendType) -> Unit,
    onOpenAuth: () -> Unit,
    onOpenApiKeys: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(YslamBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Tagline
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(YslamSurfaceVariant)
                    .border(1.dp, YslamCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "YSLAM",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = YslamCyan
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // History Quick Button
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(YslamSurface)
                    .border(1.dp, YslamBorder, RoundedCornerShape(6.dp))
                    .testTag("open_history_top_button")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Chat History",
                    tint = YslamCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // API Keys Quick Button
            IconButton(
                onClick = onOpenApiKeys,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(YslamSurface)
                    .border(1.dp, YslamBorder, RoundedCornerShape(6.dp))
                    .testTag("open_api_keys_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API Keys",
                    tint = YslamAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Right side: Active Backend & Model Pill Dropdown + User Avatar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(YslamSurface)
                        .border(1.dp, YslamBorder, RoundedCornerShape(20.dp))
                        .clickable { showMenu = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("backend_status_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (activeBackend == BackendType.GEMINI) Icons.Default.Cloud else Icons.Default.Computer,
                        contentDescription = null,
                        tint = if (activeBackend == BackendType.GEMINI) YslamCyan else YslamAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = activeModel,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = YslamTextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    val dotColor = when {
                        healthStatus == null -> YslamAmber
                        healthStatus.isHealthy -> YslamGreen
                        else -> YslamRed
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )

                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Switch Backend",
                        tint = YslamTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(YslamSurface)
                        .border(1.dp, YslamBorder, RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, tint = YslamCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gemini Cloud", color = YslamTextPrimary, fontSize = 13.sp)
                                }
                                Text("gemini-3.8-flash", color = YslamTextMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
                            }
                        },
                        onClick = {
                            onSwitchBackend(BackendType.GEMINI)
                            showMenu = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Computer, contentDescription = null, tint = YslamAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Localhost Ollama", color = YslamTextPrimary, fontSize = 13.sp)
                                }
                                Text("10.0.2.2:11434 / LAN", color = YslamTextMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
                            }
                        },
                        onClick = {
                            onSwitchBackend(BackendType.LOCAL)
                            showMenu = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // User Profile / Auth Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (userDisplayName != null) YslamPurple else YslamSurfaceVariant)
                    .border(1.dp, if (userDisplayName != null) YslamPurple else YslamBorder, CircleShape)
                    .clickable { onOpenAuth() }
                    .padding(5.dp)
                    .testTag("user_profile_button"),
                contentAlignment = Alignment.Center
            ) {
                if (userDisplayName != null) {
                    Text(
                        text = userDisplayName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = YslamBackground
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Sign In",
                        tint = YslamTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
