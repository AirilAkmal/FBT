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

class UsersNotificationActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var notificationsContainer: LinearLayout
    private lateinit var firestoreManager: FirestoreManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.users_notification)

        firestoreManager = FirestoreManager()
        auth = FirebaseAuth.getInstance()

        backButton = findViewById(R.id.backButton)
        notificationsContainer = findViewById(R.id.notificationsContainer)

        backButton.setOnClickListener { finish() }
        loadNotifications()

        // Mark as read
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        prefs.edit().putLong("lastViewedNotification", System.currentTimeMillis()).apply()
    }

    private fun loadNotifications() {
        notificationsContainer.removeAllViews()

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        firestoreManager.getUserRequests(
            userId = userId,
            onSuccess = { requests ->
                runOnUiThread {
                    val notifications = requests.filter {
                        val status = it["status"] as? String ?: ""
                        status == "APPROVED" || status == "REJECTED"
                    }

                    if (notifications.isEmpty()) {
                        val tv = TextView(this)
                        tv.text = "No notifications yet"
                        tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
                        tv.textSize = 16f
                        notificationsContainer.addView(tv)
                        return@runOnUiThread
                    }

                    for (req in notifications) {
                        val view = layoutInflater.inflate(R.layout.item_notification, notificationsContainer, false)
                        view.findViewById<TextView>(R.id.notifTitle).text = req["item"] as? String ?: ""
                        view.findViewById<TextView>(R.id.notifPurpose).text = "Purpose: ${req["purpose"]}"

                        val status = req["status"] as? String ?: ""
                        val dateView = view.findViewById<TextView>(R.id.notifDate)

                        if (status == "APPROVED") {
                            val daysLeft = calculateDaysLeft(req["returningDate"] as? String ?: "")
                            val daysText = when {
                                daysLeft < 0 -> "${-daysLeft} days OVERDUE"
                                daysLeft == 0L -> "DUE TODAY"
                                else -> "$daysLeft days left"
                            }
                            dateView.text = "✓ APPROVED • $daysText"
                            dateView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        } else {
                            val reason = req["rejectReason"] as? String ?: ""
                            if (reason.isNotEmpty()) {
                                dateView.text = "✗ REJECTED • $reason"
                            } else {
                                dateView.text = "✗ REJECTED"
                            }
                            dateView.setTextColor(android.graphics.Color.parseColor("#F44336"))
                        }

                        // Hide delete button
                        view.findViewById<ImageView>(R.id.deleteNotifButton).visibility = android.view.View.GONE

                        notificationsContainer.addView(view)
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