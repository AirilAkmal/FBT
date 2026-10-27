package com.example.fbt2

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

class LecturerHistoryActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var historyContainer: LinearLayout
    private lateinit var firestoreManager: FirestoreManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.users_history)  // Reuse users_history layout

        firestoreManager = FirestoreManager()
        auth = FirebaseAuth.getInstance()

        backButton = findViewById(R.id.backButton)
        historyContainer = findViewById(R.id.historyContainer)

        backButton.setOnClickListener { finish() }
        loadHistory()
    }

    private fun loadHistory() {
        historyContainer.removeAllViews()

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // ═══════════════════════════════════════════════
        // USE getUserRequests() INSTEAD OF getAllRequests()
        // ═══════════════════════════════════════════════
        firestoreManager.getUserRequests(
            userId = userId,
            onSuccess = { requests ->
                runOnUiThread {
                    if (requests.isEmpty()) {
                        val tv = TextView(this)
                        tv.text = "No history found"
                        tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
                        historyContainer.addView(tv)
                        return@runOnUiThread
                    }

                    for (req in requests) {
                        val view = layoutInflater.inflate(R.layout.item_request, historyContainer, false)
                        view.findViewById<TextView>(R.id.itemName).text = req["item"] as? String ?: ""
                        view.findViewById<TextView>(R.id.itemPurpose).text =
                            "Qty: ${req["quantity"]} • ${req["purpose"]}"
                        view.findViewById<TextView>(R.id.itemBD).text = "B.D: ${req["borrowingDate"]}"
                        view.findViewById<TextView>(R.id.itemRD).text = "R.D: ${req["returningDate"]}"

                        val status = req["status"] as? String ?: "PENDING"
                        val approvedBy = req["approvedBy"] as? String ?: ""

                        val approvedByView = view.findViewById<TextView>(R.id.itemApprovedBy)
                        if (approvedBy.isNotEmpty() && (status == "APPROVED" || status == "REJECTED")) {
                            approvedByView.visibility = android.view.View.VISIBLE
                            approvedByView.text = "By: $approvedBy"
                        } else {
                            approvedByView.visibility = android.view.View.GONE
                        }

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
                        }

                        val rejectReason = req["rejectReason"] as? String ?: ""
                        val rejectReasonView = view.findViewById<TextView>(R.id.itemRejectReason)
                        if (status == "REJECTED" && rejectReason.isNotEmpty()) {
                            rejectReasonView.visibility = android.view.View.VISIBLE
                            rejectReasonView.text = "Reason: $rejectReason"
                        } else {
                            rejectReasonView.visibility = android.view.View.GONE
                        }

                        // Hide delete button per lecturer feedback
                        view.findViewById<ImageView>(R.id.deleteItemButton).visibility = android.view.View.GONE

                        historyContainer.addView(view)
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