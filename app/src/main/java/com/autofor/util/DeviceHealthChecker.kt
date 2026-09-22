package com.autofor.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class DeviceHealthStatus(
    val hasCallPhone: Boolean,
    val hasNotification: Boolean,
    val hasExactAlarm: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
    val canDrawOverlays: Boolean
) {
    val isFullyConfigured: Boolean
        get() = hasCallPhone && hasNotification && hasExactAlarm && isIgnoringBatteryOptimizations && canDrawOverlays

    val hasCriticalPermissions: Boolean
        get() = hasCallPhone && hasNotification && hasExactAlarm
}

object DeviceHealthChecker {

    fun checkHealth(context: Context): DeviceHealthStatus {
        return DeviceHealthStatus(
            hasCallPhone = hasCallPhonePermission(context),
            hasNotification = hasNotificationPermission(context),
            hasExactAlarm = hasExactAlarmPermission(context),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context),
            canDrawOverlays = canDrawOverlays(context)
        )
    }

    fun hasCallPhonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun createBatteryOptimizationIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
    }

    fun createExactAlarmSettingsIntent(packageName: String): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
        } else {
            createAppSettingsIntent(packageName)
        }
    }

    fun createOverlaySettingsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
    }

    fun createAppSettingsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    }

    fun createNotificationSettingsIntent(packageName: String): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            createAppSettingsIntent(packageName)
        }
    }
}
