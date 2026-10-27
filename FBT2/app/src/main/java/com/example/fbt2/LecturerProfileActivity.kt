package com.example.fbt2

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LecturerProfileActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var saveButton: Button
    private lateinit var nameInput: EditText
    private lateinit var idNumberInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var togglePassword: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_profile)

        // 1. Initialize Firebase instead of DatabaseHelper
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        backButton = findViewById(R.id.backButton)
        saveButton = findViewById(R.id.saveButton)
        nameInput = findViewById(R.id.nameInput)
        idNumberInput = findViewById(R.id.idNumberInput)
        phoneInput = findViewById(R.id.phoneInput)
        passwordInput = findViewById(R.id.passwordInput)
        emailInput = findViewById(R.id.emailInput)
        togglePassword = findViewById(R.id.togglePassword)

        // 2. Show dummy password (Firebase doesn't store real passwords)
        passwordInput.setText("••••••••")
        passwordInput.isEnabled = false

        loadUserData()

        // 3. Move Password Toggle OUTSIDE the save button!
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

        backButton.setOnClickListener { finish() }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val email = emailInput.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 4. Save to Firestore
            saveUserData(name, email, phone)
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    nameInput.setText(document.getString("fullName") ?: "")
                    idNumberInput.setText(document.getString("idNumber") ?: "")
                    phoneInput.setText(document.getString("phone") ?: "")
                    emailInput.setText(document.getString("email") ?: "")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData(name: String, email: String, phone: String) {
        val uid = auth.currentUser?.uid ?: return

        val updates = mapOf(
            "fullName" to name,
            "phone" to phone,
            "email" to email
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}