package com.example.fbt2

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var registerButton: Button
    private lateinit var roleSpinner: Spinner
    private lateinit var fullNameInput: EditText
    private lateinit var idNumberInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var togglePassword: ImageView

    private lateinit var authManager: FirebaseAuthManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        authManager = FirebaseAuthManager(this)

        backButton = findViewById(R.id.backButton)
        registerButton = findViewById(R.id.registerButton)
        roleSpinner = findViewById(R.id.roleSpinner)
        fullNameInput = findViewById(R.id.fullNameInput)
        idNumberInput = findViewById(R.id.idNumberInput)
        emailInput = findViewById(R.id.emailInput)
        phoneInput = findViewById(R.id.phoneInput)
        passwordInput = findViewById(R.id.passwordInput)
        togglePassword = findViewById(R.id.togglePassword)

        val roles = arrayOf("Student", "Lecturer", "Admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter

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

        registerButton.setOnClickListener {
            val role = roleSpinner.selectedItem.toString()
            val fullName = fullNameInput.text.toString().trim()
            val idNumber = idNumberInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (fullName.isEmpty() || idNumber.isEmpty() || email.isEmpty() ||
                phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerButton.isEnabled = false
            registerButton.text = "REGISTERING..."

            authManager.registerUser(
                email = email,
                password = password,
                role = role,
                fullName = fullName,
                idNumber = idNumber,
                phone = phone,
                onSuccess = {
                    runOnUiThread {
                        Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()

                        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("uid", authManager.getCurrentUserId())
                            putString("email", email)
                            putString("role", role)
                            putString("fullName", fullName)
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
                        registerButton.isEnabled = true
                        registerButton.text = "REGISTER"

                        val message = when {
                            error.contains("already in use") -> "This email is already registered. Please login."
                            error.contains("network") -> "Network error. Check internet."
                            error.contains("invalid") -> "Invalid email format."
                            else -> "Registration failed: $error"
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}