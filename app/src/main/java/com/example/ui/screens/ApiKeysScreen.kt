package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.util.SecureApiKey
import com.example.util.SecureKeyStorage
import java.util.UUID

@Composable
fun ApiKeysScreen(
    keyStorage: SecureKeyStorage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var keysList by remember { mutableStateOf(keyStorage.getKeys()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Map of keyId -> boolean whether revealed
    val revealedKeys = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YslamBackground)
            .padding(16.dp)
            .testTag("api_keys_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SECURE API KEYS",
                    style = MaterialTheme.typography.labelSmall,
                    color = YslamCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hardware Encrypted (AES-256-GCM)",
                    fontSize = 11.sp,
                    color = YslamTextMuted
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = YslamCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_api_key_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = YslamBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Key", color = YslamBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Notice Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CodeBackground)
                .border(1.dp, YslamCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = YslamCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keys stored here are encrypted using hardware-backed Android KeyStore AES-GCM encryption. " +
                            "You can store custom Gemini API keys or Localhost bearer tokens to authenticate requests.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = YslamTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (keysList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = YslamTextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No API keys configured",
                        fontWeight = FontWeight.SemiBold,
                        color = YslamTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'Add Key' above to store your Gemini or Local API credentials.",
                        color = YslamTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(keysList, key = { it.id }) { item ->
                    val isRevealed = revealedKeys[item.id] == true
                    val displayText = if (isRevealed) {
                        keyStorage.getPlaintextKey(item.id) ?: item.maskedValue
                    } else {
                        item.maskedValue
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(YslamSurface)
                            .border(1.dp, YslamBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                            .testTag("api_key_card_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (item.type == "GEMINI") YslamCyan.copy(alpha = 0.2f) else YslamAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.type,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.type == "GEMINI") YslamCyan else YslamAmber
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = YslamTextPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = item.isActive,
                                    onCheckedChange = {
                                        keyStorage.toggleKeyActive(item.id)
                                        keysList = keyStorage.getKeys()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = YslamBackground,
                                        checkedTrackColor = YslamCyan,
                                        uncheckedThumbColor = YslamTextMuted,
                                        uncheckedTrackColor = YslamSurfaceVariant
                                    ),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        keyStorage.deleteKey(item.id)
                                        keysList = keyStorage.getKeys()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Key",
                                        tint = YslamRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Key Value Display with reveal and copy
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CodeBackground)
                                .border(1.dp, YslamBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayText,
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                color = if (isRevealed) YslamCyan else YslamTextSecondary,
                                modifier = Modifier.weight(1f)
                            )

                            Row {
                                IconButton(
                                    onClick = { revealedKeys[item.id] = !isRevealed },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = YslamTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = {
                                        val plaintext = keyStorage.getPlaintextKey(item.id) ?: ""
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("API Key", plaintext))
                                        Toast.makeText(context, "API Key copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Key",
                                        tint = YslamTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddApiKeyDialog(
            onConfirm = { type, label, secret ->
                keyStorage.saveKey(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    label = label,
                    secretKey = secret
                )
                keysList = keyStorage.getKeys()
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun AddApiKeyDialog(
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf("GEMINI") }
    var label by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(YslamSurface)
            .border(1.dp, YslamBorder, RoundedCornerShape(16.dp))
            .testTag("add_key_dialog"),
        title = {
            Text(
                text = "Add Secure API Key",
                color = YslamTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(text = "Target Backend", fontSize = 12.sp, color = YslamTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("GEMINI", "LOCAL")
                    types.forEach { t ->
                        val isSelected = selectedType == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) YslamSurfaceVariant else YslamBackground)
                                .border(1.dp, if (isSelected) YslamCyan else YslamBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedType = t }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (t == "GEMINI") "Google Gemini" else "Local / Ollama",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) YslamCyan else YslamTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Key Label (e.g. My Prod Key)", color = YslamTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = YslamTextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YslamCyan,
                        unfocusedBorderColor = YslamBorder,
                        focusedContainerColor = YslamBackground,
                        unfocusedContainerColor = YslamBackground
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Secret Key / Token", color = YslamTextSecondary) },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle",
                                tint = YslamTextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono, color = YslamTextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YslamCyan,
                        unfocusedBorderColor = YslamBorder,
                        focusedContainerColor = YslamBackground,
                        unfocusedContainerColor = YslamBackground
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (secret.isNotBlank()) {
                        onConfirm(
                            selectedType,
                            label.ifBlank { if (selectedType == "GEMINI") "Gemini Key" else "Local Auth Token" },
                            secret.trim()
                        )
                    }
                },
                enabled = secret.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = YslamCyan)
            ) {
                Text("Encrypt & Save", color = YslamBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = YslamTextMuted)
            }
        }
    )
}
