package com.example.ui.components.permission

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.util.permission.AppPermission
import com.example.util.permission.HapticFeedbackHelper
import com.example.util.permission.NotificationHelper
import com.example.util.permission.PermissionHelper

@Composable
fun PermissionsCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var micGranted by remember { mutableStateOf(PermissionHelper.isPermissionGranted(context, AppPermission.RECORD_AUDIO)) }
    var notifGranted by remember { mutableStateOf(PermissionHelper.isPermissionGranted(context, AppPermission.POST_NOTIFICATIONS)) }
    var vibrateGranted by remember { mutableStateOf(PermissionHelper.isPermissionGranted(context, AppPermission.VIBRATE)) }
    var internetGranted by remember { mutableStateOf(PermissionHelper.isPermissionGranted(context, AppPermission.INTERNET)) }

    var selectedRationalePermission by remember { mutableStateOf<AppPermission?>(null) }
    var isPermanentlyDeniedRationale by remember { mutableStateOf(false) }

    fun refreshStatuses() {
        micGranted = PermissionHelper.isPermissionGranted(context, AppPermission.RECORD_AUDIO)
        notifGranted = PermissionHelper.isPermissionGranted(context, AppPermission.POST_NOTIFICATIONS)
        vibrateGranted = PermissionHelper.isPermissionGranted(context, AppPermission.VIBRATE)
        internetGranted = PermissionHelper.isPermissionGranted(context, AppPermission.INTERNET)
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted) {
            HapticFeedbackHelper.vibrateSuccess(context)
            Toast.makeText(context, "Microphone permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            val shouldRationale = PermissionHelper.shouldShowRationale(activity, AppPermission.RECORD_AUDIO)
            if (!shouldRationale) {
                isPermanentlyDeniedRationale = true
                selectedRationalePermission = AppPermission.RECORD_AUDIO
            }
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        if (granted) {
            HapticFeedbackHelper.vibrateSuccess(context)
            NotificationHelper.sendTestNotification(context)
            Toast.makeText(context, "Notifications enabled! Sent test notification.", Toast.LENGTH_SHORT).show()
        } else {
            val shouldRationale = PermissionHelper.shouldShowRationale(activity, AppPermission.POST_NOTIFICATIONS)
            if (!shouldRationale) {
                isPermanentlyDeniedRationale = true
                selectedRationalePermission = AppPermission.POST_NOTIFICATIONS
            }
        }
    }

    val multipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        refreshStatuses()
        HapticFeedbackHelper.vibrateShort(context)
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(context, "All requested permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    // Refresh when screen becomes visible
    LaunchedEffect(Unit) {
        refreshStatuses()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(YslamSurface)
            .border(1.dp, YslamBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("permissions_management_card")
    ) {
        // Card Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(YslamCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Permissions",
                        tint = YslamCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ANDROID PERMISSIONS & PRIVACY",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = YslamCyan
                    )
                    Text(
                        text = "Device Capabilities & Security",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = YslamTextPrimary
                    )
                }
            }

            // Quick App Settings action
            OutlinedButton(
                onClick = { PermissionHelper.openAppSettings(context) },
                modifier = Modifier
                    .height(32.dp)
                    .testTag("open_system_app_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = YslamTextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "App Info",
                    fontSize = 11.sp,
                    fontFamily = Inter,
                    color = YslamTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Yslam requests only the minimum runtime permissions necessary for voice prompting and background task alerts. You maintain full granular control.",
            fontFamily = Inter,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = YslamTextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Permission Items List
        // 1. Microphone (RECORD_AUDIO)
        PermissionRowItem(
            icon = Icons.Default.Mic,
            title = "Voice Dictation (RECORD_AUDIO)",
            description = "Transcribes speech directly to chat messages and code prompts.",
            isGranted = micGranted,
            isDangerous = true,
            onRequestClick = {
                val shouldRationale = PermissionHelper.shouldShowRationale(activity, AppPermission.RECORD_AUDIO)
                if (shouldRationale) {
                    isPermanentlyDeniedRationale = false
                    selectedRationalePermission = AppPermission.RECORD_AUDIO
                } else {
                    micLauncher.launch(AppPermission.RECORD_AUDIO.permissionId)
                }
            },
            onRationaleClick = {
                isPermanentlyDeniedRationale = false
                selectedRationalePermission = AppPermission.RECORD_AUDIO
            },
            testTag = "perm_row_record_audio"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Notifications (POST_NOTIFICATIONS)
        PermissionRowItem(
            icon = Icons.Default.Notifications,
            title = "Background Alerts (POST_NOTIFICATIONS)",
            description = "Alerts when generation or local Ollama benchmarks finish.",
            isGranted = notifGranted,
            isDangerous = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            onRequestClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val shouldRationale = PermissionHelper.shouldShowRationale(activity, AppPermission.POST_NOTIFICATIONS)
                    if (shouldRationale) {
                        isPermanentlyDeniedRationale = false
                        selectedRationalePermission = AppPermission.POST_NOTIFICATIONS
                    } else {
                        notifLauncher.launch(AppPermission.POST_NOTIFICATIONS.permissionId)
                    }
                } else {
                    NotificationHelper.sendTestNotification(context)
                    refreshStatuses()
                }
            },
            onRationaleClick = {
                isPermanentlyDeniedRationale = false
                selectedRationalePermission = AppPermission.POST_NOTIFICATIONS
            },
            extraAction = if (notifGranted) {
                {
                    OutlinedButton(
                        onClick = {
                            HapticFeedbackHelper.vibrateShort(context)
                            val sent = NotificationHelper.sendTestNotification(context)
                            Toast.makeText(
                                context,
                                if (sent) "Notification sent!" else "Notification failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("test_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = YslamCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Alert", fontSize = 10.sp, color = YslamCyan)
                    }
                }
            } else null,
            testTag = "perm_row_post_notifications"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Haptic Vibration (VIBRATE)
        PermissionRowItem(
            icon = Icons.Default.Vibration,
            title = "Haptic Feedback (VIBRATE)",
            description = "Subtle tactile pulses on model response and mic actions.",
            isGranted = vibrateGranted,
            isDangerous = false,
            onRequestClick = null,
            onRationaleClick = null,
            extraAction = {
                OutlinedButton(
                    onClick = {
                        HapticFeedbackHelper.vibrateSuccess(context)
                        Toast.makeText(context, "Haptic pulse triggered!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("test_haptics_button")
                ) {
                    Text("Test Pulse", fontSize = 10.sp, color = YslamPurple)
                }
            },
            testTag = "perm_row_vibrate"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Network / Internet (INTERNET)
        PermissionRowItem(
            icon = Icons.Default.Wifi,
            title = "Network Access (INTERNET)",
            description = "Allows secure requests to Gemini cloud & local Ollama loopback.",
            isGranted = internetGranted,
            isDangerous = false,
            onRequestClick = null,
            onRationaleClick = null,
            testTag = "perm_row_internet"
        )

        // Bulk Request Button if any dangerous permission missing
        val hasMissingDangerous = !micGranted || (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        if (hasMissingDangerous) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    val permissionsToRequest = mutableListOf<String>()
                    if (!micGranted) permissionsToRequest.add(AppPermission.RECORD_AUDIO.permissionId)
                    if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(AppPermission.POST_NOTIFICATIONS.permissionId)
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        multipleLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("request_all_permissions_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YslamCyan,
                    contentColor = YslamBackground
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Request All Android Runtime Permissions",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }

    if (selectedRationalePermission != null) {
        val perm = selectedRationalePermission!!
        PermissionRationaleDialog(
            permission = perm,
            isPermanentlyDenied = isPermanentlyDeniedRationale,
            onRequestPermission = {
                if (perm == AppPermission.RECORD_AUDIO) {
                    micLauncher.launch(perm.permissionId)
                } else if (perm == AppPermission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(perm.permissionId)
                }
            },
            onOpenSettings = {
                PermissionHelper.openAppSettings(context)
            },
            onDismiss = {
                selectedRationalePermission = null
                isPermanentlyDeniedRationale = false
            }
        )
    }
}

@Composable
fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isDangerous: Boolean,
    onRequestClick: (() -> Unit)?,
    onRationaleClick: (() -> Unit)?,
    extraAction: (@Composable () -> Unit)? = null,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CodeBackground)
            .border(1.dp, if (isGranted) YslamBorder else YslamAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) YslamGreen.copy(alpha = 0.15f)
                    else YslamAmber.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) YslamGreen else YslamAmber,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = YslamTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Status chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isGranted) YslamGreen.copy(alpha = 0.15f)
                            else YslamAmber.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isGranted) "GRANTED" else "NOT GRANTED",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (isGranted) YslamGreen else YslamAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                fontFamily = Inter,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = YslamTextMuted
            )
        }

        if (extraAction != null) {
            Spacer(modifier = Modifier.width(8.dp))
            extraAction()
        } else if (!isGranted && onRequestClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YslamAmber,
                    contentColor = YslamBackground
                ),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("${testTag}_grant_button")
            ) {
                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
