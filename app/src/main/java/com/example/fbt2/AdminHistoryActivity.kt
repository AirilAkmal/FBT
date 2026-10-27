package com.example.fbt2

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdminHistoryActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var historyContainer: LinearLayout
    private lateinit var firestoreManager: FirestoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_history)

        firestoreManager = FirestoreManager()

        backButton = findViewById(R.id.backButton)
        historyContainer = findViewById(R.id.historyContainer)

        backButton.setOnClickListener { finish() }
        loadHistory()
    }

    private fun loadHistory() {
        historyContainer.removeAllViews()

        firestoreManager.getAllRequests(
            onSuccess = { requests ->
                runOnUiThread {
                    // Show approved, rejected, and returned in history
                    val historyItems = requests.filter {
                        val status = it["status"] as? String ?: ""
                        status == "APPROVED" || status == "REJECTED" || status == "RETURNED" || status == "OVERDUE"
                    }

                    if (historyItems.isEmpty()) {
                        val tv = TextView(this)
                        tv.text = "No history yet"
                        tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
                        historyContainer.addView(tv)
                        return@runOnUiThread
                    }

                    for (req in historyItems) {
                        val view = layoutInflater.inflate(R.layout.item_admin_history, historyContainer, false)

                        // Set basic text fields
                        view.findViewById<TextView>(R.id.itemName).text = req["item"] as? String ?: ""
                        view.findViewById<TextView>(R.id.userName).text = "By: ${req["userName"]}"
                        view.findViewById<TextView>(R.id.itemPurpose).text = "Qty: ${req["quantity"]} • ${req["purpose"]}"
                        view.findViewById<TextView>(R.id.itemDates).text =
                            "B.D: ${req["borrowingDate"]}   R.D: ${req["returningDate"]}"

                        // ═══════════════════════════════════════════════
                        // STATUS COLOR CODING LOGIC
                        // ═══════════════════════════════════════════════
                        val statusView = view.findViewById<TextView>(R.id.itemStatus)
                        val status = req["status"] as? String ?: ""

                        when (status) {
                            "APPROVED" -> {
                                // Check if it's overdue based on returning date
                                val daysLeft = calculateDaysLeft(req["returningDate"] as? String ?: "")
                                if (daysLeft < 0) {
                                    statusView.text = "OVERDUE • ${-daysLeft} days"
                                    statusView.setTextColor(android.graphics.Color.parseColor("#F44336")) // RED
                                } else {
                                    statusView.text = "APPROVED"
                                    statusView.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // GREEN
                                }
                            }
                            "RETURNED" -> {
                                statusView.text = "✓ RETURNED"
                                statusView.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // GREEN
                            }
                            "REJECTED" -> {
                                statusView.text = "REJECTED"
                                statusView.setTextColor(android.graphics.Color.parseColor("#F44336")) // RED
                            }
                            "OVERDUE" -> {
                                statusView.text = "OVERDUE"
                                statusView.setTextColor(android.graphics.Color.parseColor("#F44336")) // RED
                            }
                            else -> {
                                statusView.text = status
                                statusView.setTextColor(android.graphics.Color.parseColor("#FF9800")) // ORANGE
                            }
                        }

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