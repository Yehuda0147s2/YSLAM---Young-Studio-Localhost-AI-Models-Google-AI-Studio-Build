package com.example.ui.components

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.Inter
import com.example.ui.theme.JetBrainsMono
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
import com.example.util.AuthManager
import com.example.viewmodel.YslamViewModel

@Composable
fun AuthDialog(
    authManager: AuthManager,
    viewModel: YslamViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by authManager.currentUser.collectAsState()
    val isLoading by authManager.isLoading.collectAsState()
    val authError by authManager.authError.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(YslamSurface)
            .border(1.dp, YslamBorder, RoundedCornerShape(16.dp))
            .testTag("auth_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = YslamCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentUser != null) "Developer Account" else "Sign In to YSLAM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = YslamTextPrimary
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = YslamTextSecondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentUser != null) {
                    val user = currentUser!!
                    // User Profile View
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(YslamSurfaceVariant)
                            .border(1.dp, YslamBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(YslamCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (user.displayName?.take(1) ?: user.email?.take(1) ?: "U").uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = YslamBackground
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName ?: "Developer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = YslamTextPrimary
                            )
                            Text(
                                text = user.email ?: "",
                                fontSize = 12.sp,
                                color = YslamTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "UID: ${user.uid.take(8)}...",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = YslamCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cloud Firestore Sync status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CodeBackground)
                            .border(1.dp, YslamGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = YslamGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cloud Firestore Sync Active: User profile & chat history stored securely in backend.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = YslamTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            authManager.signOut(coroutineScope) {
                                Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YslamRed.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YslamRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = YslamRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out", color = YslamRed, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Sign-In State
                    Text(
                        text = "Sign in to backup your chat sessions, store API keys, and sync profile settings across devices.",
                        fontSize = 13.sp,
                        color = YslamTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sign-In Button
                    Button(
                        onClick = {
                            val activity = context as? Activity
                            if (activity != null) {
                                authManager.signInWithGoogle(
                                    activity = activity,
                                    scope = coroutineScope,
                                    onSuccess = { user ->
                                        viewModel.syncUserProfileToFirestore(user)
                                        Toast.makeText(context, "Welcome ${user.displayName ?: ""}", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sign in with Google",
                                    color = Color(0xFF1F1F1F),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = authError ?: "",
                            color = YslamRed,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Informational callout on email/password
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CodeBackground)
                            .border(1.dp, YslamBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "ℹ️ Note: Standard Google Sign-In is provisioned for this application. To enable email/password authentication, activate the Email/Password provider in the Firebase Console.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = YslamTextMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = YslamCyan)
            }
        }
    )
}
