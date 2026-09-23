package com.autofor.util

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.autofor.service.AutoForAccessibilityService

data class DeviceHealthStatus(
    val hasCallPhone: Boolean,
    val hasNotification: Boolean,
    val hasExactAlarm: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
    val canDrawOverlays: Boolean,
    val isAccessibilityEnabled: Boolean = false
) {
    val isFullyConfigured: Boolean
        get() = hasCallPhone && hasNotification && hasExactAlarm && isIgnoringBatteryOptimizations && canDrawOverlays && isAccessibilityEnabled

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
            canDrawOverlays = canDrawOverlays(context),
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
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

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (AutoForAccessibilityService.isServiceRunning) return true
        val expectedComponentName = ComponentName(context, AutoForAccessibilityService::class.java).flattenToString()
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true) ||
                (componentName.contains(context.packageName) && componentName.contains("AutoForAccessibilityService"))
            ) {
                return true
            }
        }
        return false
    }

    fun createAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
}
