package com.example.fbt2

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminAnyRequestActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var requestsContainer: LinearLayout

    private lateinit var firestoreManager: FirestoreManager
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_any_request)

        firestoreManager = FirestoreManager()
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        backButton = findViewById(R.id.backButton)
        requestsContainer = findViewById(R.id.requestsContainer)

        backButton.setOnClickListener { finish() }
        loadRequests()

        // Mark as read
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        prefs.edit().putLong("lastViewedAnyRequest", System.currentTimeMillis()).apply()
    }

    private fun loadRequests() {
        requestsContainer.removeAllViews()

        firestoreManager.getAllRequests(
            onSuccess = { requests ->
                runOnUiThread {
                    val pendingRequests = requests.filter { it["status"] == "PENDING" }

                    if (pendingRequests.isEmpty()) {
                        val tv = TextView(this)
                        tv.text = "No pending requests"
                        tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
                        tv.textSize = 16f
                        requestsContainer.addView(tv)
                        return@runOnUiThread
                    }

                    for (req in pendingRequests) {
                        val view = layoutInflater.inflate(R.layout.item_admin_request, requestsContainer, false)
                        view.findViewById<TextView>(R.id.itemName).text = req["item"] as? String ?: ""
                        view.findViewById<TextView>(R.id.userName).text = "By: ${req["userName"]}"
                        view.findViewById<TextView>(R.id.itemPurpose).text = "Purpose: ${req["purpose"]}"
                        view.findViewById<TextView>(R.id.itemDates).text =
                            "Qty: ${req["quantity"]}   B.D: ${req["borrowingDate"]}   R.D: ${req["returningDate"]}"

                        val statusView = view.findViewById<TextView>(R.id.itemStatus)
                        statusView.text = "PENDING"
                        statusView.setTextColor(android.graphics.Color.parseColor("#FF9800"))

                        val requestId = req["id"] as? String ?: ""

                        view.findViewById<Button>(R.id.approveButton).setOnClickListener {
                            approveRequest(requestId, req)
                        }

                        view.findViewById<Button>(R.id.rejectButton).setOnClickListener {
                            showRejectDialog(requestId)
                        }

                        requestsContainer.addView(view)
                    }
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // APPROVE
    private fun approveRequest(requestId: String, req: Map<String, Any>) {
        val currentAdminId = auth.currentUser?.uid ?: return

        firestoreManager.getUserData(
            currentAdminId,
            onSuccess = { adminData ->
                val adminName = adminData["fullName"] as? String ?: "Admin"

                firestoreManager.updateRequestStatus(
                    requestId = requestId,
                    status = "APPROVED",
                    approvedBy = adminName,
                    onSuccess = {
                        val itemName = req["item"] as? String ?: ""
                        val quantity = (req["quantity"] as? Long)?.toInt() ?: 1

                        firestoreManager.deductItemQuantity(
                            itemName = itemName,
                            amount = quantity,
                            onSuccess = {
                                runOnUiThread {
                                    Toast.makeText(this, "Approved!", Toast.LENGTH_SHORT).show()
                                    loadRequests()
                                }
                            },
                            onFailure = { error ->
                                runOnUiThread {
                                    Toast.makeText(this, "Approved but qty error: $error", Toast.LENGTH_SHORT).show()
                                    loadRequests()
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            Toast.makeText(this, "Failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            onFailure = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // REJECT WITH REASON DIALOG
    private fun showRejectDialog(requestId: String) {
        val input = EditText(this)
        input.hint = "Enter reason for rejection"
        input.setPadding(40, 40, 40, 40)
        input.minLines = 3
        input.maxLines = 5

        AlertDialog.Builder(this)
            .setTitle("Reject Request")
            .setMessage("Please enter the reason for rejection:")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                rejectRequest(requestId, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectRequest(requestId: String, reason: String) {
        val currentAdminId = auth.currentUser?.uid ?: return

        firestoreManager.getUserData(
            currentAdminId,
            onSuccess = { adminData ->
                val adminName = adminData["fullName"] as? String ?: "Admin"

                val updates = mapOf(
                    "status" to "REJECTED",
                    "approvedBy" to adminName,
                    "rejectReason" to reason
                )

                db.collection("requests").document(requestId)
                    .update(updates)
                    .addOnSuccessListener {
                        runOnUiThread {
                            Toast.makeText(this, "Rejected!", Toast.LENGTH_SHORT).show()
                            loadRequests()
                        }
                    }
                    .addOnFailureListener { e ->
                        runOnUiThread {
                            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            onFailure = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}