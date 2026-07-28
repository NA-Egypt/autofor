package com.autofor.ui

import android.Manifest
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

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val callGranted = permissions[Manifest.permission.CALL_PHONE] ?: false
        if (!callGranted) {
            Toast.makeText(this, "CALL_PHONE permission is required to dial forwarding codes", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

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
                    }
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

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
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
