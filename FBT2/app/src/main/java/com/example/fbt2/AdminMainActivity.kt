package com.example.fbt2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminMainActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var profileButton: TextView
    private lateinit var anyRequestLayout: LinearLayout
    private lateinit var statusLayout: LinearLayout
    private lateinit var historyLayout: LinearLayout
    private lateinit var notificationLayout: LinearLayout
    private lateinit var itemsManagementLayout: LinearLayout
    private lateinit var notificationBadge: TextView
    private lateinit var anyRequestBadge: TextView
    private lateinit var statusBadge: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        backButton = findViewById(R.id.backButton)
        profileButton = findViewById(R.id.profileButton)
        anyRequestLayout = findViewById(R.id.anyRequestLayout)
        statusLayout = findViewById(R.id.statusLayout)
        historyLayout = findViewById(R.id.historyLayout)
        notificationLayout = findViewById(R.id.notificationLayout)
        itemsManagementLayout = findViewById(R.id.itemsManagementLayout)
        notificationBadge = findViewById(R.id.notificationBadge)
        anyRequestBadge = findViewById(R.id.anyRequestBadge)
        statusBadge = findViewById(R.id.statusBadge)

        loadAdminInfo()

        backButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        profileButton.setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
        anyRequestLayout.setOnClickListener {
            startActivity(Intent(this, AdminAnyRequestActivity::class.java))
        }
        statusLayout.setOnClickListener {
            startActivity(Intent(this, AdminStatusActivity::class.java))
        }
        historyLayout.setOnClickListener {
            startActivity(Intent(this, AdminHistoryActivity::class.java))
        }
        notificationLayout.setOnClickListener {
            startActivity(Intent(this, AdminNotificationActivity::class.java))
        }
        itemsManagementLayout.setOnClickListener {
            startActivity(Intent(this, AdminItemsManagementActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadAllBadges()
    }

    private fun loadAdminInfo() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                findViewById<TextView>(R.id.adminNameText).text = doc.getString("fullName") ?: "Admin"
                findViewById<TextView>(R.id.adminIdText).text = "ID: ${doc.getString("idNumber") ?: ""}"
            }
    }

    private fun loadAllBadges() {
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val lastViewedAnyRequest = prefs.getLong("lastViewedAnyRequest", 0)
        val lastViewedStatus = prefs.getLong("lastViewedStatus", 0)
        val lastViewedNotification = prefs.getLong("lastViewedNotification", 0)

        db.collection("requests")
            .get()
            .addOnSuccessListener { snapshot ->
                val anyRequestCount = snapshot.documents.count { doc ->
                    doc.getString("status") == "PENDING" &&
                            (doc.getLong("timestamp") ?: 0) > lastViewedAnyRequest
                }
                updateBadge(anyRequestBadge, anyRequestCount)

                val statusCount = snapshot.documents.count { doc ->
                    doc.getString("status") == "APPROVED" &&
                            (doc.getLong("timestamp") ?: 0) > lastViewedStatus
                }
                updateBadge(statusBadge, statusCount)

                val notifCount = snapshot.documents.count { doc ->
                    val status = doc.getString("status")
                    (status == "RETURNED" || status == "REJECTED") &&
                            (doc.getLong("timestamp") ?: 0) > lastViewedNotification
                }
                updateBadge(notificationBadge, notifCount)
            }
    }

    private fun updateBadge(badge: TextView, count: Int) {
        if (count > 0) {
            badge.visibility = View.VISIBLE
            badge.text = if (count > 9) "9+" else count.toString()
        } else {
            badge.visibility = View.GONE
        }
    }
}