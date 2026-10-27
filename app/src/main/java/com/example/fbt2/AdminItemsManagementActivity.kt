package com.example.fbt2

import android.app.AlertDialog
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminItemsManagementActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var saveButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var typeSpinner: Spinner
    private lateinit var quantityInput: EditText
    private lateinit var itemsContainer: LinearLayout
    private lateinit var firestoreManager: FirestoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_items_management)

        firestoreManager = FirestoreManager()

        backButton = findViewById(R.id.backButton)
        saveButton = findViewById(R.id.saveButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        typeSpinner = findViewById(R.id.typeSpinner)
        quantityInput = findViewById(R.id.quantityInput)
        itemsContainer = findViewById(R.id.itemsContainer)

        // Category spinner
        val categoryAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, ItemCategories.CATEGORIES
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        // Type spinner
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val category = ItemCategories.CATEGORIES[position]
                val types = ItemCategories.getTypesFor(category)
                val typeAdapter = ArrayAdapter(
                    this@AdminItemsManagementActivity,
                    android.R.layout.simple_spinner_item,
                    types
                )
                typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                typeSpinner.adapter = typeAdapter
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        backButton.setOnClickListener { finish() }

        saveButton.setOnClickListener {
            val category = categorySpinner.selectedItem.toString()
            val typeName = typeSpinner.selectedItem?.toString() ?: ""
            val qty = quantityInput.text.toString().trim()

            if (typeName.isEmpty() || qty.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firestoreManager.addItem(
                category = category,
                itemName = typeName,
                quantity = qty.toInt(),
                onSuccess = {
                    runOnUiThread {
                        Toast.makeText(this, "Item Added!", Toast.LENGTH_SHORT).show()
                        quantityInput.text?.clear()
                        loadItems()
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Failed: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        loadItems()
    }

    private fun loadItems() {
        itemsContainer.removeAllViews()

        firestoreManager.getAllItems(
            onSuccess = { items ->
                runOnUiThread {
                    if (items.isEmpty()) {
                        val tv = TextView(this)
                        tv.text = "No items yet"
                        tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"))
                        itemsContainer.addView(tv)
                        return@runOnUiThread
                    }

                    for (item in items) {
                        val view = layoutInflater.inflate(R.layout.item_row, itemsContainer, false)
                        view.findViewById<TextView>(R.id.itemCategory).text = item["category"] as? String ?: ""
                        view.findViewById<TextView>(R.id.itemName).text = item["itemName"] as? String ?: ""
                        view.findViewById<TextView>(R.id.itemQty).text = "Qty: ${item["quantity"]}"

                        val itemId = item["id"] as? String ?: ""

                        // ═══════════════════════════════════════════════
                        // EDIT BUTTON — FIXED
                        // ═══════════════════════════════════════════════
                        view.findViewById<ImageView>(R.id.editItemButton).setOnClickListener {
                            showEditDialog(item)
                        }

                        // Delete button
                        view.findViewById<ImageView>(R.id.deleteItemButton).setOnClickListener {
                            AlertDialog.Builder(this)
                                .setTitle("Delete Item")
                                .setMessage("Delete ${item["itemName"]}?")
                                .setPositiveButton("Delete") { _, _ ->
                                    firestoreManager.deleteItem(
                                        itemId,
                                        onSuccess = {
                                            runOnUiThread {
                                                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                                                loadItems()
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

                        itemsContainer.addView(view)
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

    // ═══════════════════════════════════════════════
    // EDIT DIALOG — FULL FIX
    // ═══════════════════════════════════════════════
    private fun showEditDialog(item: Map<String, Any>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_item, null)
        val dialogCategorySpinner = dialogView.findViewById<Spinner>(R.id.editCategorySpinner)
        val dialogTypeSpinner = dialogView.findViewById<Spinner>(R.id.editTypeSpinner)
        val qtyInput = dialogView.findViewById<EditText>(R.id.editItemQty)

        val currentCategory = item["category"] as? String ?: "Camera"
        val currentType = item["itemName"] as? String ?: ""
        val currentQty = (item["quantity"] as? Long)?.toInt() ?: 1

        // Category spinner
        val catAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, ItemCategories.CATEGORIES
        )
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogCategorySpinner.adapter = catAdapter

        val catIndex = ItemCategories.CATEGORIES.indexOf(currentCategory).coerceAtLeast(0)
        dialogCategorySpinner.setSelection(catIndex)

        // Load types for current category
        fun loadTypes(category: String) {
            val types = ItemCategories.getTypesFor(category)
            val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogTypeSpinner.adapter = typeAdapter

            val typeIndex = types.indexOf(currentType).coerceAtLeast(0)
            dialogTypeSpinner.setSelection(typeIndex)
        }

        loadTypes(currentCategory)

        dialogCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                loadTypes(ItemCategories.CATEGORIES[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        qtyInput.setText(currentQty.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit Item")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newCategory = dialogCategorySpinner.selectedItem.toString()
                val newType = dialogTypeSpinner.selectedItem?.toString() ?: ""
                val newQty = qtyInput.text.toString().trim().toIntOrNull() ?: 0

                if (newType.isEmpty()) {
                    Toast.makeText(this, "Type required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val itemId = item["id"] as? String ?: ""
                firestoreManager.updateItem(
                    itemId = itemId,
                    category = newCategory,
                    itemName = newType,
                    quantity = newQty,
                    onSuccess = {
                        runOnUiThread {
                            Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
                            loadItems()
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
}