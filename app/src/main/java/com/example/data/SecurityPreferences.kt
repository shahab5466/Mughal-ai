package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vault_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN = "vault_pin"
        private const val KEY_DECOY_PIN = "vault_decoy_pin"
        private const val KEY_SETUP_COMPLETED = "vault_setup_completed"
        private const val KEY_SECURITY_QUESTION = "vault_security_question"
        private const val KEY_SECURITY_ANSWER = "vault_security_answer"
        private const val KEY_AUTO_LOCK = "vault_auto_lock"
        private const val KEY_HAPTIC_FEEDBACK = "vault_haptic_feedback"
        private const val KEY_FAKE_CRASH = "vault_fake_crash"
    }

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    var pin: String
        get() = prefs.getString(KEY_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PIN, value).apply()

    var decoyPin: String
        get() = prefs.getString(KEY_DECOY_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DECOY_PIN, value).apply()

    var securityQuestion: String
        get() = prefs.getString(KEY_SECURITY_QUESTION, "What is your secret favorite word?") ?: "What is your secret favorite word?"
        set(value) = prefs.edit().putString(KEY_SECURITY_QUESTION, value).apply()

    var securityAnswer: String
        get() = prefs.getString(KEY_SECURITY_ANSWER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SECURITY_ANSWER, value).apply()

    var autoLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LOCK, value).apply()

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    var fakeCrashEnabled: Boolean
        get() = prefs.getBoolean(KEY_FAKE_CRASH, false)
        set(value) = prefs.edit().putBoolean(KEY_FAKE_CRASH, value).apply()

    fun verifyPin(input: String): UnlockResult {
        val currentPin = pin
        val currentDecoy = decoyPin

        return when {
            input == currentPin && currentPin.isNotEmpty() -> UnlockResult.MainVault
            input == currentDecoy && currentDecoy.isNotEmpty() -> UnlockResult.DecoyVault
            else -> UnlockResult.Invalid
        }
    }
}

sealed class UnlockResult {
    data object MainVault : UnlockResult()
    data object DecoyVault : UnlockResult()
    data object Invalid : UnlockResult()
}
