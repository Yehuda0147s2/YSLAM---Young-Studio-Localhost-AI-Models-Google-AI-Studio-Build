package com.example.util.permission

import android.Manifest
import android.os.Build

enum class AppPermission(
    val permissionId: String,
    val title: String,
    val shortName: String,
    val purpose: String,
    val privacyGuarantee: String,
    val isRuntime: Boolean
) {
    RECORD_AUDIO(
        permissionId = Manifest.permission.RECORD_AUDIO,
        title = "Microphone & Voice Dictation",
        shortName = "Voice Prompting",
        purpose = "Allows you to speak chat prompts, code requests, and system instructions directly into the editor instead of typing.",
        privacyGuarantee = "Your voice is transcribed on-device via Android Speech Services and is never retained or uploaded to external tracking servers.",
        isRuntime = true
    ),
    POST_NOTIFICATIONS(
        permissionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            "android.permission.POST_NOTIFICATIONS"
        },
        title = "Notifications & Background Alerts",
        shortName = "Task Alerts",
        purpose = "Alerts you when background AI generation, code synthesis, or local Ollama model benchmark tests complete while the app is in the background.",
        privacyGuarantee = "Notifications only show completion status and short snippet summaries. No personal data is transmitted.",
        isRuntime = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ),
    VIBRATE(
        permissionId = Manifest.permission.VIBRATE,
        title = "Haptic Tactile Feedback",
        shortName = "Haptics",
        purpose = "Delivers subtle tactile feedback when dictation starts, code generation finishes, or network errors occur.",
        privacyGuarantee = "Normal system capability with zero privacy impact.",
        isRuntime = false
    ),
    INTERNET(
        permissionId = Manifest.permission.INTERNET,
        title = "Internet & Network Access",
        shortName = "Network",
        purpose = "Required to communicate with Gemini cloud endpoints and host Ollama instances via local loopback.",
        privacyGuarantee = "Network traffic is strictly directed to configured AI endpoints.",
        isRuntime = false
    );

    val isDangerousRuntime: Boolean
        get() = isRuntime && (this == RECORD_AUDIO || (this == POST_NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU))
}

data class PermissionItemStatus(
    val permission: AppPermission,
    val isGranted: Boolean,
    val canRequest: Boolean = true,
    val isPermanentlyDenied: Boolean = false
)
