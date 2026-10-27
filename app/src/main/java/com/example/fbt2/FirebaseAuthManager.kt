package com.example.fbt2

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseAuthManager(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // GET CURRENT USER
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // REGISTER USER
    fun registerUser(
        email: String,
        password: String,
        role: String,
        fullName: String,
        idNumber: String,
        phone: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val userData = mapOf(
                            "uid" to uid,
                            "email" to email,
                            "role" to role,
                            "fullName" to fullName,
                            "idNumber" to idNumber,
                            "phone" to phone,
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("users").document(uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d("FirebaseAuth", "✅ User registered: $uid")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseAuth", "❌ Firestore save failed: ${e.message}")
                                onFailure("Failed to save user data: ${e.message}")
                            }
                    } else {
                        onFailure("User created but UID is null")
                    }
                } else {
                    val errorMsg = task.exception?.message ?: "Registration failed"
                    Log.e("FirebaseAuth", "❌ Registration failed: $errorMsg")
                    onFailure(errorMsg)
                }
            }
    }

    // LOGIN USER
    fun loginUser(
        email: String,
        password: String,
        onSuccess: (role: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val role = document.getString("role") ?: "Student"
                                    Log.d("FirebaseAuth", "✅ Login successful: $role")
                                    onSuccess(role)
                                } else {
                                    onFailure("User data not found")
                                }
                            }
                            .addOnFailureListener { e ->
                                onFailure("Failed to get user data: ${e.message}")
                            }
                    } else {
                        onFailure("UID is null")
                    }
                } else {
                    val errorMsg = task.exception?.message ?: "Login failed"
                    Log.e("FirebaseAuth", "❌ Login failed: $errorMsg")
                    onFailure(errorMsg)
                }
            }
    }

    // LOGOUT
    fun logout() {
        auth.signOut()
        Log.d("FirebaseAuth", "✅ Logged out")
    }

    // GET USER DATA
    fun getUserData(
        uid: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onSuccess(document.data ?: emptyMap())
                } else {
                    onFailure("User document not found")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to get user")
            }
    }

    // MARK AS READ FUNCTIONS
    fun markNotificationRead() {
        val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        prefs.edit().putLong("lastViewedNotification", System.currentTimeMillis()).apply()
    }

    fun getLastViewedNotification(): Long {
        val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        return prefs.getLong("lastViewedNotification", 0)
    }

    fun markAnyRequestRead() {
        val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        prefs.edit().putLong("lastViewedAnyRequest", System.currentTimeMillis()).apply()
    }

    fun getLastViewedAnyRequest(): Long {
        val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        return prefs.getLong("lastViewedAnyRequest", 0)
    }

    fun markStatusRead() {
        val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        prefs.edit().putLong("lastViewedStatus", System.currentTimeMillis()).apply()
    }

    fun getLastViewedStatus(): Long {
        val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        return prefs.getLong("lastViewedStatus", 0)
    }
}