package com.example.fbt2

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class UsersMyRequestActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var requestsContainer: LinearLayout
    private lateinit var firestoreManager: FirestoreManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.users_my_request)

        firestoreManager = FirestoreManager()
        auth = FirebaseAuth.getInstance()

        backButton = findViewById(R.id.backButton)
        requestsContainer = findViewById(R.id.requestsContainer)

        backButton.setOnClickListener { finish() }
        loadRequests()
    }

    private fun loadRequests() {
        requestsContainer.removeAllViews()

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        firestoreManager.getUserRequests(
            userId = userId,
            onSuccess = { requests ->
                runOnUiThread {
                    if (requests.isEmpty()) {
                        val tv = TextView(this)
                        tv.text = "No requests found"
                        tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
                        tv.textSize = 16f
                        requestsContainer.addView(tv)
                        return@runOnUiThread
                    }

                    for (req in requests) {
                        val view = layoutInflater.inflate(R.layout.item_request, requestsContainer, false)
                        view.findViewById<TextView>(R.id.itemName).text = req["item"] as? String ?: ""
                        view.findViewById<TextView>(R.id.itemPurpose).text =
                            "Qty: ${req["quantity"]} • ${req["purpose"]}"
                        view.findViewById<TextView>(R.id.itemBD).text = "B.D: ${req["borrowingDate"]}"
                        view.findViewById<TextView>(R.id.itemRD).text = "R.D: ${req["returningDate"]}"

                        val status = req["status"] as? String ?: "PENDING"
                        val approvedBy = req["approvedBy"] as? String ?: ""

                        // Approved By display
                        val approvedByView = view.findViewById<TextView>(R.id.itemApprovedBy)
                        if (approvedBy.isNotEmpty() && (status == "APPROVED" || status == "REJECTED")) {
                            approvedByView.visibility = android.view.View.VISIBLE
                            approvedByView.text = "By: $approvedBy"
                        } else {
                            approvedByView.visibility = android.view.View.GONE
                        }

                        // Status display with color
                        val statusView = view.findViewById<TextView>(R.id.itemStatus)
                        when (status) {
                            "APPROVED" -> {
                                val daysLeft = calculateDaysLeft(req["returningDate"] as? String ?: "")
                                when {
                                    daysLeft < 0 -> {
                                        statusView.text = "OVERDUE • ${-daysLeft} days"
                                        statusView.setTextColor(android.graphics.Color.parseColor("#F44336"))
                                    }
                                    daysLeft == 0L -> {
                                        statusView.text = "DUE TODAY!"
                                        statusView.setTextColor(android.graphics.Color.parseColor("#F44336"))
                                    }
                                    daysLeft <= 2 -> {
                                        statusView.text = "APPROVED • $daysLeft days left"
                                        statusView.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                                    }
                                    else -> {
                                        statusView.text = "APPROVED • $daysLeft days left"
                                        statusView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                                    }
                                }
                            }
                            "REJECTED" -> {
                                statusView.text = "REJECTED"
                                statusView.setTextColor(android.graphics.Color.parseColor("#F44336"))
                            }
                            "PENDING" -> {
                                statusView.text = "PENDING"
                                statusView.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                            }
                            "RETURNED" -> {
                                statusView.text = "✓ RETURNED"
                                statusView.setTextColor(android.graphics.Color.parseColor("#2196F3"))
                            }
                            else -> {
                                statusView.text = status
                                statusView.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
                            }
                        }

                        // ═══════════════════════════════════════════════
                        // SHOW REJECT REASON
                        // ═══════════════════════════════════════════════
                        val rejectReason = req["rejectReason"] as? String ?: ""
                        val rejectReasonView = view.findViewById<TextView>(R.id.itemRejectReason)
                        if (status == "REJECTED" && rejectReason.isNotEmpty()) {
                            rejectReasonView.visibility = android.view.View.VISIBLE
                            rejectReasonView.text = "Reason: $rejectReason"
                        } else {
                            rejectReasonView.visibility = android.view.View.GONE
                        }

                        // Delete button
                        val requestId = req["id"] as? String ?: ""
                        view.findViewById<ImageView>(R.id.deleteItemButton).setOnClickListener {
                            AlertDialog.Builder(this)
                                .setTitle("Delete Request")
                                .setMessage("Delete this request?")
                                .setPositiveButton("Delete") { _, _ ->
                                    firestoreManager.deleteRequest(
                                        requestId,
                                        onSuccess = {
                                            runOnUiThread {
                                                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                                                loadRequests()
                                            }
                                        },
                                        onFailure = { error ->
                                            runOnUiThread {
                                                Toast.makeText(this, "Failed: $error", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
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

    private fun calculateDaysLeft(returnDate: String): Long {
        return try {
            val format = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val returnCal = Calendar.getInstance().apply {
                time = format.parse(returnDate) ?: return 0
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            TimeUnit.MILLISECONDS.toDays(returnCal.timeInMillis - today.timeInMillis)
        } catch (e: Exception) { 0 }
    }
}