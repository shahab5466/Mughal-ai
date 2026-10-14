package com.example.calculator

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SecurityPreferences
import com.example.data.UnlockResult

@Composable
fun CalculatorScreen(
    securityPreferences: SecurityPreferences,
    onUnlock: (isDecoy: Boolean) -> Unit,
    onLogIntruder: (attemptPin: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = remember { CalculatorEngine() }

    var displayValue by remember { mutableStateOf("0") }
    var expressionHistory by remember { mutableStateOf("") }
    var lastEvaluated by remember { mutableStateOf(false) }

    // Dialog states
    var showSetupDialog by remember { mutableStateOf(!securityPreferences.isSetupCompleted) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showQuickHelpDialog by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        if (!securityPreferences.hapticFeedbackEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    fun onNumber(digit: String) {
        triggerHaptic()
        if (displayValue == "0" || lastEvaluated) {
            displayValue = digit
            lastEvaluated = false
        } else {
            if (displayValue.length < 15) {
                displayValue += digit
            }
        }
    }

    fun onOperator(op: String) {
        triggerHaptic()
        lastEvaluated = false
        val lastChar = displayValue.lastOrNull()
        if (lastChar != null && "+-×÷%".contains(lastChar)) {
            displayValue = displayValue.dropLast(1) + op
        } else {
            displayValue += op
        }
    }

    fun onClear() {
        triggerHaptic()
        displayValue = "0"
        expressionHistory = ""
        lastEvaluated = false
    }

    fun onBackspace() {
        triggerHaptic()
        if (displayValue.length > 1) {
            displayValue = displayValue.dropLast(1)
        } else {
            displayValue = "0"
        }
    }

    fun onToggleSign() {
        triggerHaptic()
        if (displayValue != "0") {
            displayValue = if (displayValue.startsWith("-")) {
                displayValue.substring(1)
            } else {
                "-$displayValue"
            }
        }
    }

    fun onDot() {
        triggerHaptic()
        val currentPart = displayValue.split('+', '-', '×', '÷', '%').lastOrNull() ?: ""
        if (!currentPart.contains(".")) {
            displayValue += "."
        }
    }

    fun onEquals() {
        triggerHaptic()

        // 1. Emergency recovery code
        if (displayValue == "11223344") {
            showRecoveryDialog = true
            displayValue = "0"
            return
        }

        // 2. Check if setup is not done yet
        if (!securityPreferences.isSetupCompleted) {
            if (displayValue.length in 4..8 && displayValue.all { it.isDigit() }) {
                showSetupDialog = true
            } else {
                expressionHistory = displayValue
                displayValue = engine.evaluate(displayValue)
                lastEvaluated = true
            }
            return
        }

        // 3. Check for Secret Vault unlock
        when (securityPreferences.verifyPin(displayValue)) {
            is UnlockResult.MainVault -> {
                displayValue = "0"
                onUnlock(false)
                return
            }
            is UnlockResult.DecoyVault -> {
                displayValue = "0"
                onUnlock(true)
                return
            }
            is UnlockResult.Invalid -> {
                // If pure numbers of 4+ digits and does not match PIN, log as intruder attempt
                if (displayValue.length in 4..8 && displayValue.all { it.isDigit() }) {
                    onLogIntruder(displayValue)
                }
            }
        }

        // 4. Standard calculator evaluation
        expressionHistory = displayValue
        val result = engine.evaluate(displayValue)
        displayValue = result
        lastEvaluated = true
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF111827) // Deep matte stealth background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with discreet controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Discreet Security Indicator",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                showQuickHelpDialog = true
                            }
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "RAD",
                        color = Color(0xFF4B5563),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = { showQuickHelpDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Vault Guide",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Calculation Display Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expressionHistory,
                    color = Color(0xFF9CA3AF),
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = displayValue,
                    color = Color.White,
                    fontSize = if (displayValue.length > 9) 38.sp else 52.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_display")
                )
            }

            // Keypad Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: AC, +/-, %, ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalcKey(
                        text = "AC",
                        textColor = Color(0xFFEF4444),
                        buttonColor = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f),
                        onClick = { onClear() }
                    )
                    CalcKey(
                        text = "+/-",
                        textColor = Color(0xFF60A5FA),
                        buttonColor = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f),
                        onClick = { onToggleSign() }
                    )
                    CalcKey(
                        text = "%",
                        textColor = Color(0xFF60A5FA),
                        buttonColor = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f),
                        onClick = { onOperator("%") }
                    )
                    CalcKey(
                        text = "÷",
                        textColor = Color(0xFFF59E0B),
                        buttonColor = Color(0xFF1E293B),
                        modifier = Modifier.weight(1f),
                        onClick = { onOperator("÷") }
                    )
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalcKey(
                        text = "7",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("7") }
                    )
                    CalcKey(
                        text = "8",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("8") }
                    )
                    CalcKey(
                        text = "9",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("9") }
                    )
                    CalcKey(
                        text = "×",
                        textColor = Color(0xFFF59E0B),
                        buttonColor = Color(0xFF1E293B),
                        modifier = Modifier.weight(1f),
                        onClick = { onOperator("×") }
                    )
                }

                // Row 3: 4, 5, 6, -
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalcKey(
                        text = "4",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("4") }
                    )
                    CalcKey(
                        text = "5",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("5") }
                    )
                    CalcKey(
                        text = "6",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("6") }
                    )
                    CalcKey(
                        text = "-",
                        textColor = Color(0xFFF59E0B),
                        buttonColor = Color(0xFF1E293B),
                        modifier = Modifier.weight(1f),
                        onClick = { onOperator("-") }
                    )
                }

                // Row 4: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalcKey(
                        text = "1",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("1") }
                    )
                    CalcKey(
                        text = "2",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("2") }
                    )
                    CalcKey(
                        text = "3",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("3") }
                    )
                    CalcKey(
                        text = "+",
                        textColor = Color(0xFFF59E0B),
                        buttonColor = Color(0xFF1E293B),
                        modifier = Modifier.weight(1f),
                        onClick = { onOperator("+") }
                    )
                }

                // Row 5: 0, ., ⌫, =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalcKey(
                        text = "0",
                        modifier = Modifier.weight(1f),
                        onClick = { onNumber("0") }
                    )
                    CalcKey(
                        text = ".",
                        modifier = Modifier.weight(1f),
                        onClick = { onDot() }
                    )
                    CalcKeyIcon(
                        icon = Icons.Default.Backspace,
                        modifier = Modifier.weight(1f),
                        onClick = { onBackspace() }
                    )
                    CalcKey(
                        text = "=",
                        textColor = Color.White,
                        buttonColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f),
                        onLongClick = {
                            // Long-press = opens Forgot PIN / Security Question Dialog!
                            triggerHaptic()
                            showRecoveryDialog = true
                        },
                        onClick = { onEquals() }
                    )
                }
            }
        }
    }

    // First time PIN setup dialog
    if (showSetupDialog) {
        SetupPinDialog(
            initialEntered = if (displayValue.length in 4..8 && displayValue.all { it.isDigit() }) displayValue else "",
            onPinConfigured = { pin, question, answer ->
                securityPreferences.pin = pin
                securityPreferences.securityQuestion = question
                securityPreferences.securityAnswer = answer
                securityPreferences.isSetupCompleted = true
                showSetupDialog = false
                displayValue = "0"
                onUnlock(false)
            },
            onDismiss = {
                showSetupDialog = false
            }
        )
    }

    // Forgot PIN / Security Question dialog
    if (showRecoveryDialog) {
        RecoveryDialog(
            question = securityPreferences.securityQuestion,
            actualAnswer = securityPreferences.securityAnswer,
            onSuccessReset = { newPin ->
                securityPreferences.pin = newPin
                showRecoveryDialog = false
                onUnlock(false)
            },
            onDismiss = { showRecoveryDialog = false }
        )
    }

    // Quick Help / Stealth Info dialog
    if (showQuickHelpDialog) {
        QuickHelpDialog(
            isSetup = securityPreferences.isSetupCompleted,
            onOpenSetup = {
                showQuickHelpDialog = false
                showSetupDialog = true
            },
            onDismiss = { showQuickHelpDialog = false }
        )
    }
}

@Composable
fun CalcKey(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    buttonColor: Color = Color(0xFF1F2937),
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(20.dp))
            .background(buttonColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick?.invoke() },
                    onTap = { onClick() }
                )
            }
            .testTag("key_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CalcKeyIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    buttonColor: Color = Color(0xFF1F2937),
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(20.dp))
            .background(buttonColor)
            .clickable { onClick() }
            .testTag("key_backspace"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Backspace",
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun SetupPinDialog(
    initialEntered: String,
    onPinConfigured: (pin: String, question: String, answer: String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf(initialEntered) }
    var confirmPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("What was your first school or favorite color?") }
    var answer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Set Secret PIN", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a 4-to-6 digit numeric passcode. In the future, entering this PIN and pressing '=' in the calculator will unlock your hidden vault.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("4-6 Digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_pin_input")
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_confirm_pin_input")
                )

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Security Question") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Security Answer (for password reset)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_security_answer")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length < 4) {
                        errorMessage = "PIN must be at least 4 digits"
                    } else if (pin != confirmPin) {
                        errorMessage = "PINs do not match"
                    } else if (answer.isBlank()) {
                        errorMessage = "Please provide a security answer"
                    } else {
                        onPinConfigured(pin, question, answer.trim())
                    }
                },
                modifier = Modifier.testTag("setup_save_button")
            ) {
                Text("Activate Vault")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Use Calculator Only")
            }
        }
    )
}

@Composable
fun RecoveryDialog(
    question: String,
    actualAnswer: String,
    onSuccessReset: (newPin: String) -> Unit,
    onDismiss: () -> Unit
) {
    var answerInput by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var isAnswerVerified by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Vault Recovery", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isAnswerVerified) {
                    Text(
                        text = "Answer your secret security question to reset your PIN:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3B82F6)
                    )
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it },
                        label = { Text("Your Answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("recovery_answer_input")
                    )
                } else {
                    Text(
                        text = "Answer verified! Enter a new 4-6 digit PIN:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("New PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("recovery_new_pin_input")
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isAnswerVerified) {
                        if (answerInput.trim().equals(actualAnswer.trim(), ignoreCase = true) || actualAnswer.isEmpty()) {
                            isAnswerVerified = true
                            errorMessage = null
                        } else {
                            errorMessage = "Incorrect answer. Try again."
                        }
                    } else {
                        if (newPin.length < 4) {
                            errorMessage = "PIN must be at least 4 digits"
                        } else {
                            onSuccessReset(newPin)
                        }
                    }
                }
            ) {
                Text(if (!isAnswerVerified) "Verify Answer" else "Save New PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuickHelpDialog(
    isSetup: Boolean,
    onOpenSetup: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF3B82F6))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Mughal Calculator Guide", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "• How to Unlock: Type your secret PIN and press '='.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Forgot PIN: Long-press '=' or type '11223344=' to answer your security question.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Disguise: Acts as a 100% genuine everyday calculator for everyone else.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Media Protection: Hidden photos and videos are stored in private sandboxed storage, completely hidden from your gallery.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            if (!isSetup) {
                Button(onClick = onOpenSetup) {
                    Text("Setup PIN Now")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Got It")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
