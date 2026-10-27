package com.example.fbt2

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LecturerNotificationActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var notificationsContainer: LinearLayout

    private lateinit var firestoreManager: FirestoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.users_notification)

        firestoreManager = FirestoreManager()

        backButton = findViewById(R.id.backButton)
        notificationsContainer = findViewById(R.id.notificationsContainer)

        backButton.setOnClickListener { finish() }
        loadNotifications()

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        prefs.edit().putLong("lastViewedNotification", System.currentTimeMillis()).apply()
    }

    private fun loadNotifications() {
        notificationsContainer.removeAllViews()

        firestoreManager.getAllRequests(
            onSuccess = { requests ->
                runOnUiThread {
                    val pendingRequests = requests.filter { it["status"] == "PENDING" }

                    if (pendingRequests.isEmpty()) {
                        val tv = TextView(this)
                        tv.text = "No new notifications"
                        tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
                        tv.textSize = 16f
                        notificationsContainer.addView(tv)
                        return@runOnUiThread
                    }

                    for (req in pendingRequests.take(20)) {
                        val view = layoutInflater.inflate(R.layout.item_notification, notificationsContainer, false)
                        view.findViewById<TextView>(R.id.notifTitle).text = req["item"] as? String ?: ""
                        view.findViewById<TextView>(R.id.notifPurpose).text =
                            "By: ${req["userName"]} • Qty: ${req["quantity"]}"

                        val dateView = view.findViewById<TextView>(R.id.notifDate)
                        dateView.text = "PENDING"
                        dateView.setTextColor(android.graphics.Color.parseColor("#FF9800"))

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
}