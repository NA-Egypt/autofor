package com.autofor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autofor.data.ForwardingRule

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleDialog(
    initialRule: ForwardingRule? = null,
    onDismiss: () -> Unit,
    onSave: (ForwardingRule) -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "Work Forwarding") }
    var targetPhone by remember { mutableStateOf(initialRule?.targetPhoneNumber ?: "") }
    var startHour by remember { mutableIntStateOf(initialRule?.startHour ?: 9) }
    var startMinute by remember { mutableIntStateOf(initialRule?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initialRule?.endHour ?: 17) }
    var endMinute by remember { mutableIntStateOf(initialRule?.endMinute ?: 0) }
    var selectedDays by remember { mutableStateOf(initialRule?.daysOfWeek ?: setOf(2, 3, 4, 5, 6)) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val daysMap = listOf(
        1 to "Su", 2 to "M", 3 to "Tu", 4 to "W", 5 to "Th", 6 to "F", 7 to "Sa"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule == null) "New Forwarding Rule" else "Edit Rule") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetPhone,
                    onValueChange = { targetPhone = it },
                    label = { Text("Forward To Phone Number") },
                    placeholder = { Text("+1234567890") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Repeat Days", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    daysMap.forEach { (dayInt, label) ->
                        val isSelected = selectedDays.contains(dayInt)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    selectedDays - dayInt
                                } else {
                                    selectedDays + dayInt
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Start Time", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { showStartTimePicker = true }) {
                            Text(String.format("%02d:%02d", startHour, startMinute))
                        }
                    }
                    Column {
                        Text("End Time", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { showEndTimePicker = true }) {
                            Text(String.format("%02d:%02d", endHour, endMinute))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ruleToSave = (initialRule ?: ForwardingRule()).copy(
                        name = name.ifBlank { "Forwarding Rule" },
                        targetPhoneNumber = targetPhone,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        daysOfWeek = selectedDays
                    )
                    onSave(ruleToSave)
                },
                enabled = targetPhone.isNotBlank() && selectedDays.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showStartTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startHour = timeState.hour
                    startMinute = timeState.minute
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    if (showEndTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endHour = timeState.hour
                    endMinute = timeState.minute
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}
