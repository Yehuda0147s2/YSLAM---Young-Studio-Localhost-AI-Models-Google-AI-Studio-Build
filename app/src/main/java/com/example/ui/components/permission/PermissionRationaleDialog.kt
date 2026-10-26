package com.example.ui.components.permission

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.Inter
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.YslamAmber
import com.example.ui.theme.YslamBackground
import com.example.ui.theme.YslamBorder
import com.example.ui.theme.YslamCyan
import com.example.ui.theme.YslamGreen
import com.example.ui.theme.YslamPurple
import com.example.ui.theme.YslamSurface
import com.example.ui.theme.YslamSurfaceVariant
import com.example.ui.theme.YslamTextMuted
import com.example.ui.theme.YslamTextPrimary
import com.example.ui.theme.YslamTextSecondary
import com.example.util.permission.AppPermission

@Composable
fun PermissionRationaleDialog(
    permission: AppPermission,
    isPermanentlyDenied: Boolean = false,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val icon: ImageVector = when (permission) {
        AppPermission.RECORD_AUDIO -> Icons.Default.Mic
        AppPermission.POST_NOTIFICATIONS -> Icons.Default.Notifications
        else -> Icons.Default.Security
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, YslamCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("permission_rationale_dialog"),
            color = YslamSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header badge & title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(YslamCyan.copy(alpha = 0.15f))
                            .border(1.dp, YslamCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = permission.title,
                            tint = YslamCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "ANDROID PERMISSION REQUIRED",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = YslamCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = permission.title,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = YslamTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Purpose explanation
                Text(
                    text = permission.purpose,
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = YslamTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Privacy guarantee banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(YslamBackground)
                        .border(1.dp, YslamBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy Guarantee",
                        tint = YslamGreen,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PRIVACY GUARANTEE",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = YslamGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = permission.privacyGuarantee,
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = YslamTextMuted
                        )
                    }
                }

                if (isPermanentlyDenied) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(YslamAmber.copy(alpha = 0.12f))
                            .border(1.dp, YslamAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = YslamAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permission was previously declined with 'Don't ask again'. Please enable it in Android System Settings.",
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            color = YslamAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_permission_dialog")
                    ) {
                        Text(
                            text = "Not Now",
                            fontFamily = Inter,
                            fontSize = 13.sp,
                            color = YslamTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isPermanentlyDenied) {
                        Button(
                            onClick = {
                                onOpenSettings()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YslamAmber,
                                contentColor = YslamBackground
                            ),
                            modifier = Modifier.testTag("open_settings_permission_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Settings",
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                onRequestPermission()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YslamCyan,
                                contentColor = YslamBackground
                            ),
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text(
                                text = "Allow Access",
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
