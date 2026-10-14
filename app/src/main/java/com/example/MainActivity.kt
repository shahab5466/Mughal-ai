package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.calculator.CalculatorScreen
import com.example.data.SecurityPreferences
import com.example.ui.theme.MyApplicationTheme
import com.example.vault.VaultDashboardScreen
import com.example.vault.VaultViewModel

class MainActivity : ComponentActivity() {

    private val vaultViewModel: VaultViewModel by viewModels()
    private lateinit var securityPreferences: SecurityPreferences
    private var isVaultUnlockedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        securityPreferences = SecurityPreferences(this)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = Color(0xFF0F172A)
                ) {
                    val isUnlocked by remember { isVaultUnlockedState }

                    AnimatedContent(
                        targetState = isUnlocked,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "vault_screen_switch"
                    ) { unlocked ->
                        if (unlocked) {
                            VaultDashboardScreen(
                                viewModel = vaultViewModel,
                                onLockVault = {
                                    isVaultUnlockedState.value = false
                                }
                            )
                        } else {
                            CalculatorScreen(
                                securityPreferences = securityPreferences,
                                onUnlock = { isDecoy ->
                                    vaultViewModel.setDecoyMode(isDecoy)
                                    isVaultUnlockedState.value = true
                                },
                                onLogIntruder = { attemptedPin ->
                                    vaultViewModel.logIntruderAttempt(attemptedPin)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // If user switches apps or device locks, re-lock the vault immediately
        if (::securityPreferences.isInitialized && securityPreferences.autoLockEnabled) {
            isVaultUnlockedState.value = false
        }
    }
}
