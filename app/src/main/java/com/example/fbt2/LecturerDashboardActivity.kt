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

class LecturerDashboardActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var profileButton: TextView
    private lateinit var createRequestLayout: LinearLayout
    private lateinit var historyLayout: LinearLayout
    private lateinit var notificationLayout: LinearLayout
    private lateinit var myRequestLayout: LinearLayout
    private lateinit var notificationBadge: TextView
    private lateinit var lecturerNameText: TextView
    private lateinit var lecturerIdText: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lecturer_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        backButton = findViewById(R.id.backButton)
        profileButton = findViewById(R.id.profileButton)
        createRequestLayout = findViewById(R.id.createRequestLayout)
        historyLayout = findViewById(R.id.historyLayout)
        notificationLayout = findViewById(R.id.notificationLayout)
        myRequestLayout = findViewById(R.id.myRequestLayout)
        notificationBadge = findViewById(R.id.notificationBadge)
        lecturerNameText = findViewById(R.id.lecturerNameText)
        lecturerIdText = findViewById(R.id.lecturerIdText)

        loadLecturerInfo()
        loadNotificationCount()

        backButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        profileButton.setOnClickListener {
            startActivity(Intent(this, UsersProfileActivity::class.java))
        }
        createRequestLayout.setOnClickListener {
            startActivity(Intent(this, UsersCreateRequestActivity::class.java))
        }
        historyLayout.setOnClickListener {
            startActivity(Intent(this, LecturerHistoryActivity::class.java))
        }
        notificationLayout.setOnClickListener {
            startActivity(Intent(this, LecturerNotificationActivity::class.java))
        }
        myRequestLayout.setOnClickListener {
            startActivity(Intent(this, LecturerMyRequestActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotificationCount()
    }

    private fun loadLecturerInfo() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                lecturerNameText.text = doc.getString("fullName") ?: "Lecturer"
                lecturerIdText.text = "ID: ${doc.getString("idNumber") ?: ""}"
            }
    }

    private fun loadNotificationCount() {
        val uid = auth.currentUser?.uid ?: return

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val lastViewed = prefs.getLong("lastViewedNotification", 0)

        db.collection("requests")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.documents.count { doc ->
                    val status = doc.getString("status")
                    val timestamp = doc.getLong("timestamp") ?: 0
                    (status == "APPROVED" || status == "REJECTED") && timestamp > lastViewed
                }

                if (count > 0) {
                    notificationBadge.visibility = View.VISIBLE
                    notificationBadge.text = if (count > 9) "9+" else count.toString()
                } else {
                    notificationBadge.visibility = View.GONE
                }
            }
    }
}