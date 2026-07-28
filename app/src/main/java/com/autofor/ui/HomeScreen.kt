package com.autofor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.autofor.R
import com.autofor.data.ForwardingRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isGlobalEnabled: Boolean,
    onGlobalEnabledChange: (Boolean) -> Unit,
    lastStatus: String,
    rules: List<ForwardingRule>,
    onAddRuleClick: () -> Unit,
    onEditRuleClick: (ForwardingRule) -> Unit,
    onDeleteRuleClick: (ForwardingRule) -> Unit,
    onToggleRule: (ForwardingRule, Boolean) -> Unit,
    onManualForwardClick: (phoneNumber: String) -> Unit,
    onManualCancelClick: () -> Unit
) {
    var showManualForwardDialog by remember { mutableStateOf(false) }
    var manualPhoneNumber by remember { mutableStateOf("") }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Toggle Card
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
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isGlobalEnabled) "Active background scheduling" else "All background schedules disabled",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isGlobalEnabled,
                        onCheckedChange = onGlobalEnabledChange
                    )
                }
            }

            // Current Status Banner
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.PhoneCallback, contentDescription = null)
                    Column {
                        Text("Status", style = MaterialTheme.typography.labelSmall)
                        Text(lastStatus, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Quick Manual Controls
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

            Text(
                text = "Scheduled Rules (${rules.size})",
                style = MaterialTheme.typography.titleLarge
            )

            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No schedule rules added yet.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                    Text(rule.name, style = MaterialTheme.typography.titleMedium)
                                    Text("To: ${rule.targetPhoneNumber}", style = MaterialTheme.typography.bodyMedium)
                                    Text("${rule.formatTimeRange()} • ${rule.formatDays()}", style = MaterialTheme.typography.bodySmall)
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
            }

            // Credits Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
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
}
