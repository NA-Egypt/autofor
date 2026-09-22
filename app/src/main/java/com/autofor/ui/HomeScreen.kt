package com.autofor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autofor.R
import com.autofor.data.ForwardingRule
import com.autofor.util.DeviceHealthStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isGlobalEnabled: Boolean,
    onGlobalEnabledChange: (Boolean) -> Unit,
    lastStatus: String,
    lastError: String?,
    healthStatus: DeviceHealthStatus,
    rules: List<ForwardingRule>,
    onAddRuleClick: () -> Unit,
    onEditRuleClick: (ForwardingRule) -> Unit,
    onDeleteRuleClick: (ForwardingRule) -> Unit,
    onToggleRule: (ForwardingRule, Boolean) -> Unit,
    onManualForwardClick: (phoneNumber: String) -> Unit,
    onManualCancelClick: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDismissError: () -> Unit,
    showTroubleshootingInitially: Boolean = false
) {
    var showManualForwardDialog by remember { mutableStateOf(false) }
    var manualPhoneNumber by remember { mutableStateOf("") }
    var showTroubleshootingDialog by remember { mutableStateOf(showTroubleshootingInitially) }
    var isHealthExpanded by remember { mutableStateOf(!healthStatus.isFullyConfigured) }

    LaunchedEffect(showTroubleshootingInitially) {
        if (showTroubleshootingInitially) {
            showTroubleshootingDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "AutoFor Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Text("AutoFor - Call Forwarding")
                    }
                },
                actions = {
                    IconButton(onClick = { showTroubleshootingDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Troubleshooting & Guide"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRuleClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Toggle Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automated Call Forwarding",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGlobalEnabled) "Active background scheduling" else "All background schedules paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isGlobalEnabled,
                            onCheckedChange = onGlobalEnabledChange
                        )
                    }
                }
            }

            // Current Status or Error Banner
            item {
                if (lastError != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Error")
                                Text(
                                    "Issue Detected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = lastError,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = onDismissError) {
                                    Text("Dismiss")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { showTroubleshootingDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Fix Issue")
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PhoneCallback, contentDescription = null)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Current Status", style = MaterialTheme.typography.labelSmall)
                                Text(lastStatus, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Device Health & System Reliability Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (healthStatus.isFullyConfigured) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHealthExpanded = !isHealthExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (healthStatus.isFullyConfigured) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (healthStatus.isFullyConfigured) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Reliability & Permissions",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (healthStatus.isFullyConfigured) {
                                            "100% ready for background execution"
                                        } else {
                                            "Tap to review setup for uninterrupted forwarding"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { isHealthExpanded = !isHealthExpanded }) {
                                Icon(
                                    imageVector = if (isHealthExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Checklist"
                                )
                            }
                        }

                        AnimatedVisibility(visible = isHealthExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider()

                                PermissionHealthItem(
                                    title = "Phone Calling (CALL_PHONE)",
                                    description = "Required to dial forwarding MMI codes",
                                    isGranted = healthStatus.hasCallPhone,
                                    onFix = onRequestPhonePermission
                                )

                                PermissionHealthItem(
                                    title = "Exact Alarms (SCHEDULE_EXACT_ALARM)",
                                    description = "Ensures rules trigger at the exact minute",
                                    isGranted = healthStatus.hasExactAlarm,
                                    onFix = onRequestExactAlarm
                                )

                                PermissionHealthItem(
                                    title = "Battery Saver Exemption",
                                    description = "Prevents Android from putting AutoFor to sleep",
                                    isGranted = healthStatus.isIgnoringBatteryOptimizations,
                                    onFix = onRequestBatteryOptimization
                                )

                                PermissionHealthItem(
                                    title = "Display Over Other Apps",
                                    description = "Allows automated dialing while screen is locked",
                                    isGranted = healthStatus.canDrawOverlays,
                                    onFix = onRequestOverlayPermission
                                )

                                PermissionHealthItem(
                                    title = "Notifications",
                                    description = "Alerts and 1-tap dial action if screen is locked",
                                    isGranted = healthStatus.hasNotification,
                                    onFix = onRequestNotificationPermission
                                )
                            }
                        }
                    }
                }
            }

            // Quick Manual Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showManualForwardDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Forward Now")
                    }
                    OutlinedButton(
                        onClick = onManualCancelClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel Forwarding")
                    }
                }
            }

            // Rules Header
            item {
                Text(
                    text = "Scheduled Rules (${rules.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Rules List or Empty State
            if (rules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No schedule rules added yet.\nTap + below to create your first rule.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Forward to: ${rule.targetPhoneNumber}", style = MaterialTheme.typography.bodyMedium)
                                Text("${rule.formatTimeRange()} • ${rule.formatDays()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { onToggleRule(rule, it) }
                                )
                                IconButton(onClick = { onEditRuleClick(rule) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { onDeleteRuleClick(rule) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }

            // Credits Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "In loving service by NA Egypt",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Manual Forward Dialog
    if (showManualForwardDialog) {
        AlertDialog(
            onDismissRequest = { showManualForwardDialog = false },
            title = { Text("Manual Call Forwarding") },
            text = {
                OutlinedTextField(
                    value = manualPhoneNumber,
                    onValueChange = { manualPhoneNumber = it },
                    label = { Text("Target Phone Number") },
                    placeholder = { Text("+1234567890") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onManualForwardClick(manualPhoneNumber)
                        showManualForwardDialog = false
                    },
                    enabled = manualPhoneNumber.isNotBlank()
                ) {
                    Text("Dial *21*")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualForwardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Comprehensive Troubleshooting Dialog
    if (showTroubleshootingDialog) {
        AlertDialog(
            onDismissRequest = { showTroubleshootingDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
            title = { Text("Troubleshooting & Setup Guide") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (lastError != null) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Last Failure: $lastError",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "To ensure AutoFor executes forwarding smoothly without missing scheduled times:",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    item {
                        TroubleshootingStep(
                            number = "1",
                            title = "Disable Battery Optimization",
                            detail = "Android kills apps running in the background to save battery. Setting AutoFor to 'Unrestricted' lets it wake up exactly on time.",
                            actionLabel = "Open Battery Settings",
                            onAction = onRequestBatteryOptimization
                        )
                    }

                    item {
                        TroubleshootingStep(
                            number = "2",
                            title = "Allow 'Display over other apps'",
                            detail = "On Android 10+, apps cannot dial or open windows while the phone is locked unless granted this permission.",
                            actionLabel = "Open Overlay Settings",
                            onAction = onRequestOverlayPermission
                        )
                    }

                    item {
                        TroubleshootingStep(
                            number = "3",
                            title = "Alarms & Reminders",
                            detail = "Ensure Exact Alarms are permitted in Android settings so scheduled triggers fire without delay.",
                            actionLabel = "Open Alarm Settings",
                            onAction = onRequestExactAlarm
                        )
                    }

                    item {
                        TroubleshootingStep(
                            number = "4",
                            title = "Default Voice SIM",
                            detail = "If your phone has Dual SIMs, ensure a Default SIM is configured for calls under Android SIM settings so dialing doesn't get stuck on a prompt."
                        )
                    }

                    item {
                        TroubleshootingStep(
                            number = "5",
                            title = "Carrier MMI Code Support",
                            detail = "Test dialing *21*<number># directly from your phone app to confirm your carrier supports GSM unconditional call forwarding."
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTroubleshootingDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun PermissionHealthItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isGranted) {
            FilledTonalButton(
                onClick = onFix,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Fix", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TroubleshootingStep(
    number: String,
    title: String,
    detail: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$number. $title",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
