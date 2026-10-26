package com.example.util.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun isPermissionGranted(context: Context, permission: AppPermission): Boolean {
        return when (permission) {
            AppPermission.RECORD_AUDIO -> {
                ContextCompat.checkSelfPermission(
                    context,
                    permission.permissionId
                ) == PackageManager.PERMISSION_GRANTED
            }
            AppPermission.POST_NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        permission.permissionId
                    ) == PackageManager.PERMISSION_GRANTED &&
                            NotificationManagerCompat.from(context).areNotificationsEnabled()
                } else {
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
            }
            AppPermission.VIBRATE, AppPermission.INTERNET -> {
                ContextCompat.checkSelfPermission(
                    context,
                    permission.permissionId
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun isPermissionGranted(context: Context, permissionString: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permissionString
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowRationale(activity: Activity?, permission: AppPermission): Boolean {
        if (activity == null || !permission.isDangerousRuntime) return false
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.permissionId)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    }

    fun getAllStatuses(context: Context, activity: Activity? = null): List<PermissionItemStatus> {
        return AppPermission.values().map { permission ->
            val granted = isPermissionGranted(context, permission)
            val shouldRationale = shouldShowRationale(activity, permission)
            PermissionItemStatus(
                permission = permission,
                isGranted = granted,
                canRequest = !granted,
                isPermanentlyDenied = !granted && !shouldRationale && activity != null
            )
        }
    }
}
