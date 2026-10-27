package com.example.fbt2

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class UsersCreateRequestActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var submitButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var itemSpinner: Spinner
    private lateinit var purposeInput: EditText
    private lateinit var borrowingDateInput: EditText
    private lateinit var returningDateInput: EditText
    private lateinit var quantityText: TextView
    private lateinit var quantityInput: EditText

    private lateinit var firestoreManager: FirestoreManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.users_new_request)

        firestoreManager = FirestoreManager()
        auth = FirebaseAuth.getInstance()

        backButton = findViewById(R.id.backButton)
        submitButton = findViewById(R.id.submitButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        itemSpinner = findViewById(R.id.itemSpinner)
        purposeInput = findViewById(R.id.purposeInput)
        borrowingDateInput = findViewById(R.id.borrowingDateInput)
        returningDateInput = findViewById(R.id.returningDateInput)
        quantityText = findViewById(R.id.quantityText)
        quantityInput = findViewById(R.id.quantityInput)

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ItemCategories.CATEGORIES)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val category = ItemCategories.CATEGORIES[position]
                val types = ItemCategories.getTypesFor(category)
                val typeAdapter = ArrayAdapter(this@UsersCreateRequestActivity, android.R.layout.simple_spinner_item, types)
                typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                itemSpinner.adapter = typeAdapter
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        itemSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedType = itemSpinner.selectedItem?.toString() ?: ""
                if (selectedType.isEmpty()) return

                //Item available Qty
                firestoreManager.getItemQuantity(
                    selectedType,
                    onSuccess = { qty ->
                        runOnUiThread {
                            quantityText.text = "Available: $qty"
                            quantityText.setTextColor(
                                if (qty > 0) android.graphics.Color.parseColor("#4CAF50")
                                else android.graphics.Color.parseColor("#F44336")
                            )
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            quantityText.text = "Available: 0"
                            quantityText.setTextColor(android.graphics.Color.parseColor("#F44336"))
                        }
                    }
                )
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        backButton.setOnClickListener { finish() }
        borrowingDateInput.setOnClickListener { showDatePicker(borrowingDateInput) }
        returningDateInput.setOnClickListener { showDatePicker(returningDateInput) }

        submitButton.setOnClickListener {
            val category = categorySpinner.selectedItem.toString()
            val item = itemSpinner.selectedItem?.toString() ?: ""
            val qtyStr = quantityInput.text.toString().trim()
            val purpose = purposeInput.text.toString().trim()
            val borrowingDate = borrowingDateInput.text.toString().trim()
            val returningDate = returningDateInput.text.toString().trim()

            if (item.isEmpty() || qtyStr.isEmpty() || purpose.isEmpty() ||
                borrowingDate.isEmpty() || returningDate.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val requestedQty = qtyStr.toIntOrNull() ?: 0
            if (requestedQty <= 0) {
                Toast.makeText(this, "Quantity must be at least 1", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // CONFIRMATION DIALOG
                        AlertDialog.Builder(this)
                .setTitle("Confirm Borrowing")
                .setMessage("Are you sure want to borrow this?\n\n" +
                        "⚠ When you borrow any items you are responsible for it. " +
                        "Damage and need to pay back if lost or broken.")
                .setPositiveButton("Yes, Borrow") { _, _ ->
                    proceedWithRequest(category, item, requestedQty, purpose, borrowingDate, returningDate)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun proceedWithRequest(
        category: String, item: String, requestedQty: Int,
        purpose: String, borrowingDate: String, returningDate: String
    ) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        firestoreManager.getItemQuantity(
            item,
            onSuccess = { available ->
                if (available < requestedQty) {
                    runOnUiThread {
                        Toast.makeText(this, "Only $available available", Toast.LENGTH_SHORT).show()
                    }
                    return@getItemQuantity
                }

                firestoreManager.getUserData(
                    user.uid,
                    onSuccess = { userData ->
                        val userName = userData["fullName"] as? String ?: "User"
                        val userEmail = userData["email"] as? String ?: ""

                        firestoreManager.createRequest(
                            userId = user.uid,
                            userName = userName,
                            userEmail = userEmail,
                            category = category,
                            item = item,
                            quantity = requestedQty,
                            purpose = purpose,
                            borrowingDate = borrowingDate,
                            returningDate = returningDate,
                            onSuccess = {
                                runOnUiThread {
                                    Toast.makeText(this, "Request Submitted!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            },
                            onFailure = { error ->
                                runOnUiThread {
                                    Toast.makeText(this, "Failed to check stock: $error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            Toast.makeText(this, "Error, Failed to load your profile: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            onFailure = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Error, Failed to submit request: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    //Date to pop up
    private fun showDatePicker(inputField: EditText) {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                inputField.setText(String.format("%02d/%02d/%d", day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }
}