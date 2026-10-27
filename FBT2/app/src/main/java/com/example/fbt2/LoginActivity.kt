package com.example.fbt2

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var loginButton: Button
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var togglePassword: ImageView

    private lateinit var authManager: FirebaseAuthManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authManager = FirebaseAuthManager(this)

        backButton = findViewById(R.id.backButton)
        loginButton = findViewById(R.id.loginButton)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        togglePassword = findViewById(R.id.togglePassword)

        togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordInput.setSelection(passwordInput.text?.length ?: 0)
            togglePassword.setColorFilter(
                if (isPasswordVisible) android.graphics.Color.parseColor("#4CAF50")
                else android.graphics.Color.parseColor("#FFD700")
            )
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            loginButton.text = "LOGGING IN..."

            authManager.loginUser(
                email = email,
                password = password,
                onSuccess = { role ->
                    runOnUiThread {
                        Toast.makeText(this, "Login Successful! Role: $role", Toast.LENGTH_SHORT).show()

                        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("uid", authManager.getCurrentUserId())
                            putString("email", email)
                            putString("role", role)
                            apply()
                        }

                        val intent = when (role) {
                            "Admin" -> Intent(this, AdminMainActivity::class.java)
                            "Lecturer" -> Intent(this, LecturerDashboardActivity::class.java)
                            else -> Intent(this, UsersMainActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        loginButton.isEnabled = true
                        loginButton.text = "LOGIN"

                        val message = when {
                            error.contains("password") -> "Wrong password"
                            error.contains("no user") -> "Email not registered"
                            error.contains("network") -> "Network error. Check internet."
                            else -> "Login failed: $error"
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}