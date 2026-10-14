package com.example.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IntruderAttempt
import com.example.data.SecurityPreferences

@Composable
fun SettingsScreen(
    securityPreferences: SecurityPreferences,
    intruderLogs: List<IntruderAttempt>,
    onClearIntruders: () -> Unit,
    onLockVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDecoyPinDialog by remember { mutableStateOf(false) }
    var showIntruderDialog by remember { mutableStateOf(false) }
    var showRecoveryConfigDialog by remember { mutableStateOf(false) }

    var autoLock by remember { mutableStateOf(securityPreferences.autoLockEnabled) }
    var haptic by remember { mutableStateOf(securityPreferences.hapticFeedbackEnabled) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Security & Passcode
        Text(
            text = "SECURITY & ACCESS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF60A5FA),
            letterSpacing = 1.sp
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.Pin,
                    title = "Change Master PIN",
                    subtitle = "Current PIN: **** (tap to update)",
                    onClick = { showChangePinDialog = true }
                )

                HorizontalDivider(color = Color(0xFF334155))

                SettingsItem(
                    icon = Icons.Default.VisibilityOff,
                    title = "Decoy Vault PIN (Plausible Deniability)",
                    subtitle = if (securityPreferences.decoyPin.isEmpty()) "Not configured (tap to set fake PIN)" else "Decoy PIN active",
                    onClick = { showDecoyPinDialog = true }
                )

                HorizontalDivider(color = Color(0xFF334155))

                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Security Recovery Question",
                    subtitle = securityPreferences.securityQuestion,
                    onClick = { showRecoveryConfigDialog = true }
                )
            }
        }

        // Section: Stealth & Intruder Alerts
        Text(
            text = "STEALTH & MONITORING",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF60A5FA),
            letterSpacing = 1.sp
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.Warning,
                    title = "Intruder Attempt Logs",
                    subtitle = "${intruderLogs.size} suspicious attempts recorded",
                    onClick = { showIntruderDialog = true }
                )

                HorizontalDivider(color = Color(0xFF334155))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Lock on App Exit", color = Color.White, fontWeight = FontWeight.Medium)
                        Text(
                            "Instantly re-locks calculator when switching away from app",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoLock,
                        onCheckedChange = {
                            autoLock = it
                            securityPreferences.autoLockEnabled = it
                        }
                    )
                }

                HorizontalDivider(color = Color(0xFF334155))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Calculator Haptic Feedback", color = Color.White, fontWeight = FontWeight.Medium)
                        Text(
                            "Vibrate subtly on calculator key presses",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = haptic,
                        onCheckedChange = {
                            haptic = it
                            securityPreferences.hapticFeedbackEnabled = it
                        }
                    )
                }
            }
        }

        // Action: Lock Vault
        Button(
            onClick = onLockVault,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_lock_now_btn")
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Lock Vault & Disguise Now")
        }
    }

    // Change Master PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            currentActualPin = securityPreferences.pin,
            onSave = { newPin ->
                securityPreferences.pin = newPin
                showChangePinDialog = false
            },
            onDismiss = { showChangePinDialog = false }
        )
    }

    // Decoy PIN Dialog
    if (showDecoyPinDialog) {
        DecoyPinDialog(
            currentDecoy = securityPreferences.decoyPin,
            masterPin = securityPreferences.pin,
            onSave = { newDecoy ->
                securityPreferences.decoyPin = newDecoy
                showDecoyPinDialog = false
            },
            onDismiss = { showDecoyPinDialog = false }
        )
    }

    // Intruder Logs Dialog
    if (showIntruderDialog) {
        IntruderLogsDialog(
            logs = intruderLogs,
            onClear = {
                onClearIntruders()
                showIntruderDialog = false
            },
            onDismiss = { showIntruderDialog = false }
        )
    }

    // Recovery config dialog
    if (showRecoveryConfigDialog) {
        RecoveryConfigDialog(
            currentQuestion = securityPreferences.securityQuestion,
            currentAnswer = securityPreferences.securityAnswer,
            onSave = { q, a ->
                securityPreferences.securityQuestion = q
                securityPreferences.securityAnswer = a
                showRecoveryConfigDialog = false
            },
            onDismiss = { showRecoveryConfigDialog = false }
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF60A5FA),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
    }
}

@Composable
fun ChangePinDialog(
    currentActualPin: String,
    onSave: (newPin: String) -> Unit,
    onDismiss: () -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Master PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it },
                    label = { Text("Current PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("change_old_pin")
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("New 4-6 Digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("change_new_pin")
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("change_confirm_pin")
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPin != currentActualPin) {
                        error = "Incorrect current PIN"
                    } else if (newPin.length < 4) {
                        error = "New PIN must be at least 4 digits"
                    } else if (newPin != confirmPin) {
                        error = "New PINs do not match"
                    } else {
                        onSave(newPin)
                    }
                }
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DecoyPinDialog(
    currentDecoy: String,
    masterPin: String,
    onSave: (newDecoy: String) -> Unit,
    onDismiss: () -> Unit
) {
    var decoyInput by remember { mutableStateOf(currentDecoy) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decoy Vault PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "If someone forces you to unlock your calculator, you can enter this Decoy PIN instead of your real PIN. It will open an empty fake vault for plausible deniability!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = decoyInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) decoyInput = it },
                    label = { Text("Decoy PIN (e.g. 0000)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("decoy_pin_input")
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (decoyInput.isNotEmpty() && decoyInput == masterPin) {
                        error = "Decoy PIN cannot be identical to Master PIN"
                    } else if (decoyInput.isNotEmpty() && decoyInput.length < 4) {
                        error = "Must be at least 4 digits or empty to disable"
                    } else {
                        onSave(decoyInput)
                    }
                }
            ) {
                Text("Save Decoy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun IntruderLogsDialog(
    logs: List<IntruderAttempt>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Intruder Attempts Log", fontWeight = FontWeight.Bold) },
        text = {
            if (logs.isEmpty()) {
                Text("No suspicious unauthorized attempts recorded. Your vault is safe!")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The following incorrect PINs were entered in the calculator:", fontSize = 12.sp)
                    logs.take(10).forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Entered: '${log.attemptedPin}'", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            Text(formatDate(log.timestamp), fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (logs.isNotEmpty()) {
                Button(onClick = onClear) {
                    Text("Clear Logs")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RecoveryConfigDialog(
    currentQuestion: String,
    currentAnswer: String,
    onSave: (q: String, a: String) -> Unit,
    onDismiss: () -> Unit
) {
    var question by remember { mutableStateOf(currentQuestion) }
    var answer by remember { mutableStateOf(currentAnswer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Security Question", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Answer") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (answer.isNotBlank()) {
                        onSave(question.trim(), answer.trim())
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
