package ir.lbo.locationsms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class LoginActivity : AppCompatActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var passwordInput: EditText
    private lateinit var confirmInput: EditText
    private lateinit var titleText: TextView
    private lateinit var errorText: TextView
    private lateinit var submitButton: Button
    private lateinit var biometricButton: Button

    private var isSettingPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        settings = SettingsRepository(this)

        passwordInput = findViewById(R.id.passwordInput)
        confirmInput = findViewById(R.id.confirmInput)
        titleText = findViewById(R.id.loginTitle)
        errorText = findViewById(R.id.errorText)
        submitButton = findViewById(R.id.submitButton)
        biometricButton = findViewById(R.id.biometricButton)

        isSettingPassword = !settings.isPasswordSet()

        if (isSettingPassword) {
            titleText.text = "برای ورود به برنامه یک رمز عبور تعیین کنید"
            confirmInput.visibility = android.view.View.VISIBLE
            submitButton.text = "تعیین رمز و ورود"
            biometricButton.visibility = android.view.View.GONE
        } else {
            titleText.text = "برای ورود، رمز عبور را وارد کنید"
            confirmInput.visibility = android.view.View.GONE
            submitButton.text = "ورود"
            biometricButton.visibility =
                if (canUseBiometric()) android.view.View.VISIBLE else android.view.View.GONE
        }

        submitButton.setOnClickListener { onSubmit() }
        biometricButton.setOnClickListener { showBiometricPrompt() }
    }

    override fun onStart() {
        super.onStart()
        // Offer biometric immediately when returning to the login screen,
        // so the user doesn't have to tap the button every time.
        if (!isSettingPassword && canUseBiometric()) {
            showBiometricPrompt()
        }
    }

    private fun canUseBiometric(): Boolean {
        if (!settings.isBiometricEnabled()) return false
        return BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                proceedToMain()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // User can still fall back to typing the password; no need to show an error.
            }
        }

        val prompt = BiometricPrompt(this, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ورود با اثر انگشت")
            .setSubtitle("برای ورود به My Car Tracker انگشت خود را روی سنسور قرار دهید")
            .setNegativeButtonText("استفاده از رمز عبور")
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun onSubmit() {
        val password = passwordInput.text.toString()
        errorText.text = ""

        if (password.length < 4) {
            errorText.text = "رمز عبور باید حداقل ۴ کاراکتر باشد"
            return
        }

        if (isSettingPassword) {
            val confirm = confirmInput.text.toString()
            if (password != confirm) {
                errorText.text = "تکرار رمز عبور مطابقت ندارد"
                return
            }
            settings.setPassword(password)
            proceedToMain()
        } else {
            if (settings.verifyPassword(password)) {
                proceedToMain()
            } else {
                errorText.text = "رمز عبور اشتباه است"
                passwordInput.text?.clear()
            }
        }
    }

    private fun proceedToMain() {
        AppLockState.unlocked = true
        startActivity(Intent(this, ModeSelectionActivity::class.java))
        finish()
    }
}
