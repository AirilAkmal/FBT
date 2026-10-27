package com.example.fbt2

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LecturerStatusActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var statusContainer: LinearLayout

    private lateinit var firestoreManager: FirestoreManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_status)

        firestoreManager = FirestoreManager()
        auth = FirebaseAuth.getInstance()

        backButton = findViewById(R.id.backButton)
        statusContainer = findViewById(R.id.statusContainer)

        backButton.setOnClickListener { finish() }
        loadApprovedRequests()

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        prefs.edit().putLong("lastViewedStatus", System.currentTimeMillis()).apply()
    }

    private fun loadApprovedRequests() {
        statusContainer.removeAllViews()

        firestoreManager.getAllRequests(
            onSuccess = { requests ->
                runOnUiThread {
                    val approved = requests.filter { it["status"] == "APPROVED" }

                    if (approved.isEmpty()) {
                        val tv = TextView(this)
                        tv.text = "No active borrowings"
                        tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
                        tv.textSize = 16f
                        statusContainer.addView(tv)
                        return@runOnUiThread
                    }

                    for (req in approved) {
                        val view = layoutInflater.inflate(R.layout.item_status, statusContainer, false)
                        view.findViewById<TextView>(R.id.itemName).text = req["item"] as? String ?: ""
                        view.findViewById<TextView>(R.id.userName).text = "By: ${req["userName"]}"
                        view.findViewById<TextView>(R.id.itemDates).text =
                            "Qty: ${req["quantity"]}   B.D: ${req["borrowingDate"]}   R.D: ${req["returningDate"]}"

                        val requestId = req["id"] as? String ?: ""

                        view.findViewById<Button>(R.id.returnButton).setOnClickListener {
                            markReturned(requestId, req)
                        }

                        statusContainer.addView(view)
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

    private fun markReturned(requestId: String, req: Map<String, Any>) {
        val lecturerId = auth.currentUser?.uid ?: return

        firestoreManager.getUserData(
            lecturerId,
            onSuccess = { lecturerData ->
                val lecturerName = lecturerData["fullName"] as? String ?: "Lecturer"
                val itemName = req["item"] as? String ?: ""
                val quantity = (req["quantity"] as? Long)?.toInt() ?: 1

                firestoreManager.updateRequestStatus(
                    requestId = requestId,
                    status = "RETURNED",
                    approvedBy = lecturerName,
                    onSuccess = {
                        firestoreManager.increaseItemQuantity(
                            itemName = itemName,
                            amount = quantity,
                            onSuccess = {
                                runOnUiThread {
                                    Toast.makeText(this, "Returned $quantity item(s)", Toast.LENGTH_SHORT).show()
                                    loadApprovedRequests()
                                }
                            },
                            onFailure = { error ->
                                runOnUiThread {
                                    Toast.makeText(this, "Returned but qty error: $error", Toast.LENGTH_SHORT).show()
                                    loadApprovedRequests()
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
}