package com.example.ui.components.permission

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.YslamAmber
import com.example.ui.theme.YslamCyan
import com.example.ui.theme.YslamRed
import com.example.ui.theme.YslamSurface
import com.example.ui.theme.YslamSurfaceVariant
import com.example.ui.theme.YslamTextMuted
import com.example.util.permission.AppPermission
import com.example.util.permission.HapticFeedbackHelper
import com.example.util.permission.PermissionHelper
import com.example.util.permission.SpeechRecognitionHelper
import com.example.util.permission.SpeechState

@Composable
fun VoiceDictationButton(
    onTextAppended: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val speechHelper = remember { SpeechRecognitionHelper(context) }
    val speechState by speechHelper.state.collectAsState()

    var showRationaleDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            HapticFeedbackHelper.vibrateSuccess(context)
            speechHelper.startListening { text ->
                onTextAppended(text)
            }
        } else {
            val canShowRationale = PermissionHelper.shouldShowRationale(activity, AppPermission.RECORD_AUDIO)
            if (!canShowRationale) {
                isPermanentlyDenied = true
                showRationaleDialog = true
            } else {
                Toast.makeText(
                    context,
                    "Microphone permission is needed for voice prompting",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
        }
    }

    val isListening = speechState is SpeechState.Listening || speechState is SpeechState.Processing

    // Pulsing animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun handleMicClick() {
        HapticFeedbackHelper.vibrateShort(context)
        if (isListening) {
            speechHelper.stopListening()
            return
        }

        val granted = PermissionHelper.isPermissionGranted(context, AppPermission.RECORD_AUDIO)
        if (granted) {
            speechHelper.startListening { text ->
                onTextAppended(text)
            }
        } else {
            val shouldRationale = PermissionHelper.shouldShowRationale(activity, AppPermission.RECORD_AUDIO)
            if (shouldRationale) {
                isPermanentlyDenied = false
                showRationaleDialog = true
            } else {
                permissionLauncher.launch(AppPermission.RECORD_AUDIO.permissionId)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        AnimatedVisibility(visible = isListening) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(YslamRed.copy(alpha = 0.15f))
                    .border(1.dp, YslamRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(YslamRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (speechState is SpeechState.Processing) "Processing..." else "Listening...",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = YslamRed
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        IconButton(
            onClick = { handleMicClick() },
            modifier = Modifier
                .size(if (compact) 40.dp else 48.dp)
                .scale(if (isListening) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    when {
                        isListening -> YslamRed
                        else -> YslamSurfaceVariant
                    }
                )
                .border(
                    1.dp,
                    if (isListening) YslamRed else YslamCyan.copy(alpha = 0.4f),
                    CircleShape
                )
                .testTag("voice_dictation_button")
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Dictation" else "Dictate Prompt with Voice",
                tint = if (isListening) Color.White else YslamCyan,
                modifier = Modifier.size(if (compact) 20.dp else 22.dp)
            )
        }
    }

    if (showRationaleDialog) {
        PermissionRationaleDialog(
            permission = AppPermission.RECORD_AUDIO,
            isPermanentlyDenied = isPermanentlyDenied,
            onRequestPermission = {
                permissionLauncher.launch(AppPermission.RECORD_AUDIO.permissionId)
            },
            onOpenSettings = {
                PermissionHelper.openAppSettings(context)
            },
            onDismiss = { showRationaleDialog = false }
        )
    }
}
