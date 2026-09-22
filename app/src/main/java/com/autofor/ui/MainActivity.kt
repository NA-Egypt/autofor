package com.autofor.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.autofor.data.ForwardingRule
import com.autofor.ui.theme.AutoForTheme
import com.autofor.util.DeviceHealthChecker

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SHOW_TROUBLESHOOTING = "extra_show_troubleshooting"
    }

    private val viewModel: HomeViewModel by viewModels()

    private var openTroubleshootingTrigger by mutableStateOf(false)

    private val requestPhonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(this, "CALL_PHONE permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "CALL_PHONE permission is required to dial forwarding codes", Toast.LENGTH_LONG).show()
        }
        viewModel.loadData()
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Notification permission is needed for scheduled alerts", Toast.LENGTH_LONG).show()
        }
        viewModel.loadData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)
        checkEssentialPermissions()

        setContent {
            AutoForTheme {
                val uiState by viewModel.uiState.collectAsState()

                var editingRule by remember { mutableStateOf<ForwardingRule?>(null) }
                var showRuleDialog by remember { mutableStateOf(false) }

                HomeScreen(
                    isGlobalEnabled = uiState.isGlobalEnabled,
                    onGlobalEnabledChange = { enabled ->
                        viewModel.setGlobalEnabled(enabled)
                    },
                    lastStatus = uiState.lastStatus,
                    lastError = uiState.lastError,
                    healthStatus = uiState.healthStatus,
                    rules = uiState.rules,
                    onAddRuleClick = {
                        editingRule = null
                        showRuleDialog = true
                    },
                    onEditRuleClick = { rule ->
                        editingRule = rule
                        showRuleDialog = true
                    },
                    onDeleteRuleClick = { rule ->
                        viewModel.deleteRule(rule.id)
                    },
                    onToggleRule = { rule, enabled ->
                        viewModel.toggleRule(rule, enabled)
                    },
                    onManualForwardClick = { phoneNumber ->
                        executeManualForwarding(phoneNumber)
                    },
                    onManualCancelClick = {
                        executeManualCancel()
                    },
                    onRequestPhonePermission = {
                        requestPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    },
                    onRequestExactAlarm = {
                        launchSettingsIntentSafely(DeviceHealthChecker.createExactAlarmSettingsIntent(packageName))
                    },
                    onRequestBatteryOptimization = {
                        launchSettingsIntentSafely(DeviceHealthChecker.createBatteryOptimizationIntent(packageName))
                    },
                    onRequestOverlayPermission = {
                        launchSettingsIntentSafely(DeviceHealthChecker.createOverlaySettingsIntent(packageName))
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            launchSettingsIntentSafely(DeviceHealthChecker.createNotificationSettingsIntent(packageName))
                        }
                    },
                    onDismissError = {
                        viewModel.clearError()
                    },
                    showTroubleshootingInitially = openTroubleshootingTrigger
                )

                if (showRuleDialog) {
                    RuleDialog(
                        initialRule = editingRule,
                        onDismiss = { showRuleDialog = false },
                        onSave = { ruleToSave ->
                            viewModel.addOrUpdateRule(ruleToSave)
                            showRuleDialog = false
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_SHOW_TROUBLESHOOTING, false) == true) {
            openTroubleshootingTrigger = true
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    private fun checkEssentialPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun launchSettingsIntentSafely(settingsIntent: Intent) {
        try {
            startActivity(settingsIntent)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(DeviceHealthChecker.createAppSettingsIntent(packageName))
            } catch (fallbackEx: Exception) {
                Toast.makeText(this, "Could not open system settings: ${fallbackEx.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Settings error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun executeManualForwarding(phoneNumber: String) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val mmiCode = "*21*$cleanNumber#"
        dialMmiCode(mmiCode, "Manual forwarding to $phoneNumber")
    }

    private fun executeManualCancel() {
        val mmiCode = "#21#"
        dialMmiCode(mmiCode, "Manual call forwarding cancelled")
    }

    private fun dialMmiCode(mmiCode: String, statusMsg: String) {
        val encodedCode = Uri.encode(mmiCode)
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$encodedCode"))
        try {
            startActivity(intent)
            viewModel.updateStatus(statusMsg)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to execute $mmiCode: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
