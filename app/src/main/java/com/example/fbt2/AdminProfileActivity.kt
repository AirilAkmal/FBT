package com.example.fbt2

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class AdminProfileActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var saveButton: Button
    private lateinit var changePasswordButton: Button
    private lateinit var nameInput: EditText
    private lateinit var idNumberInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var toggleNewPassword: ImageView
    private lateinit var toggleConfirmPassword: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        backButton = findViewById(R.id.backButton)
        saveButton = findViewById(R.id.saveButton)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        nameInput = findViewById(R.id.nameInput)
        idNumberInput = findViewById(R.id.idNumberInput)
        phoneInput = findViewById(R.id.phoneInput)
        emailInput = findViewById(R.id.emailInput)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        toggleNewPassword = findViewById(R.id.toggleNewPassword)
        toggleConfirmPassword = findViewById(R.id.toggleConfirmPassword)

        loadUserData()

        // ═══════════════════════════════════════════════
        // EYE ICON 1: Toggle New Password
        // ═══════════════════════════════════════════════
        toggleNewPassword.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            if (isNewPasswordVisible) {
                newPasswordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                newPasswordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            newPasswordInput.setSelection(newPasswordInput.text?.length ?: 0)
            toggleNewPassword.setColorFilter(
                if (isNewPasswordVisible) android.graphics.Color.parseColor("#4CAF50")
                else android.graphics.Color.parseColor("#FFD700")
            )
        }

        // ═══════════════════════════════════════════════
        // EYE ICON 2: Toggle Confirm Password
        // ═══════════════════════════════════════════════
        toggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                confirmPasswordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                confirmPasswordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            confirmPasswordInput.setSelection(confirmPasswordInput.text?.length ?: 0)
            toggleConfirmPassword.setColorFilter(
                if (isConfirmPasswordVisible) android.graphics.Color.parseColor("#4CAF50")
                else android.graphics.Color.parseColor("#FFD700")
            )
        }

        backButton.setOnClickListener { finish() }

        saveButton.setOnClickListener { saveUserData() }

        changePasswordButton.setOnClickListener { changePassword() }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
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

    private fun saveUserData() {
        val userId = auth.currentUser?.uid ?: return
        val name = nameInput.text.toString().trim()
        val idNumber = idNumberInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()

        if (name.isEmpty() || idNumber.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "fullName" to name,
            "idNumber" to idNumber,
            "phone" to phone
        )

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun changePassword() {
        val newPass = newPasswordInput.text.toString().trim()
        val confirmPass = confirmPasswordInput.text.toString().trim()

        // Validation
        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirmPass) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Update password in Firebase Auth
        user.updatePassword(newPass)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Password Changed Successfully!", Toast.LENGTH_LONG).show()
                newPasswordInput.text?.clear()
                confirmPasswordInput.text?.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}