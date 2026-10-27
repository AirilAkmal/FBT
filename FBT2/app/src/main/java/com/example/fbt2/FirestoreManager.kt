package com.example.fbt2

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirestoreManager {

    private val db = FirebaseFirestore.getInstance()

    // ITEMS — Add, Read, Update, Delete
    fun addItem(
        category: String,
        itemName: String,
        quantity: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val itemData = mapOf(
            "category" to category,
            "itemName" to itemName,
            "quantity" to quantity,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("items")
            .add(itemData)
            .addOnSuccessListener { docRef ->
                Log.d("FirestoreManager", "✅ Item added: ${docRef.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreManager", "❌ Add item failed: ${e.message}")
                onFailure(e.message ?: "Failed to add item")
            }
    }

    fun getAllItems(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("items")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val items = mutableListOf<Map<String, Any>>()
                for (doc in querySnapshot.documents) {
                    val data = doc.data?.toMutableMap() ?: continue
                    data["id"] = doc.id
                    items.add(data)
                }
                Log.d("FirestoreManager", "✅ Loaded ${items.size} items")
                onSuccess(items)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreManager", "❌ Load items failed: ${e.message}")
                onFailure(e.message ?: "Failed to load items")
            }
    }

    fun getItemQuantity(
        itemName: String,
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("items")
            .whereEqualTo("itemName", itemName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val qty = doc.getLong("quantity")?.toInt() ?: 0
                    onSuccess(qty)
                } else {
                    onSuccess(0)
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to get quantity")
            }
    }

    fun deductItemQuantity(
        itemName: String,
        amount: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("items")
            .whereEqualTo("itemName", itemName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val currentQty = doc.getLong("quantity")?.toInt() ?: 0
                    val newQty = (currentQty - amount).coerceAtLeast(0)

                    db.collection("items").document(doc.id)
                        .update("quantity", newQty)
                        .addOnSuccessListener {
                            Log.d("FirestoreManager", "✅ Quantity deducted: $currentQty → $newQty")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.message ?: "Failed to deduct")
                        }
                } else {
                    onFailure("Item not found")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to find item")
            }
    }

    fun increaseItemQuantity(
        itemName: String,
        amount: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("items")
            .whereEqualTo("itemName", itemName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val currentQty = doc.getLong("quantity")?.toInt() ?: 0
                    val newQty = currentQty + amount

                    db.collection("items").document(doc.id)
                        .update("quantity", newQty)
                        .addOnSuccessListener {
                            Log.d("FirestoreManager", "✅ Quantity increased: $currentQty → $newQty")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.message ?: "Failed to increase")
                        }
                } else {
                    onFailure("Item not found")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to find item")
            }
    }

    fun updateItem(
        itemId: String,
        category: String,
        itemName: String,
        quantity: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = mapOf(
            "category" to category,
            "itemName" to itemName,
            "quantity" to quantity
        )

        db.collection("items").document(itemId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("FirestoreManager", "✅ Item updated")
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update")
            }
    }

    fun deleteItem(
        itemId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("items").document(itemId)
            .delete()
            .addOnSuccessListener {
                Log.d("FirestoreManager", "✅ Item deleted")
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to delete")
            }
    }

    // REQUESTS — Create, Read, Update
    fun createRequest(
        userId: String,
        userName: String,
        userEmail: String,
        category: String,
        item: String,
        quantity: Int,
        purpose: String,
        borrowingDate: String,
        returningDate: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val requestData = mapOf(
            "userId" to userId,
            "userName" to userName,
            "userEmail" to userEmail,
            "category" to category,
            "item" to item,
            "quantity" to quantity,
            "purpose" to purpose,
            "borrowingDate" to borrowingDate,
            "returningDate" to returningDate,
            "status" to "PENDING",
            "approvedBy" to "",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("requests")
            .add(requestData)
            .addOnSuccessListener { docRef ->
                Log.d("FirestoreManager", "✅ Request created: ${docRef.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreManager", "❌ Create request failed: ${e.message}")
                onFailure(e.message ?: "Failed to create request")
            }
    }

    fun getAllRequests(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("requests")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val requests = mutableListOf<Map<String, Any>>()
                for (doc in querySnapshot.documents) {
                    val data = doc.data?.toMutableMap() ?: continue
                    data["id"] = doc.id
                    requests.add(data)
                }
                Log.d("FirestoreManager", "✅ Loaded ${requests.size} requests")
                onSuccess(requests)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to load requests")
            }
    }

    fun getUserRequests(
        userId: String,
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("requests")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val requests = mutableListOf<Map<String, Any>>()
                for (doc in querySnapshot.documents) {
                    val data = doc.data?.toMutableMap() ?: continue
                    data["id"] = doc.id
                    requests.add(data)
                }
                onSuccess(requests)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to load requests")
            }
    }

    fun updateRequestStatus(
        requestId: String,
        status: String,
        approvedBy: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = mapOf(
            "status" to status,
            "approvedBy" to approvedBy
        )

        db.collection("requests").document(requestId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("FirestoreManager", "✅ Request $requestId updated to $status")
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to update status")
            }
    }
    fun getUserData(
        uid: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onSuccess(document.data ?: emptyMap())
                } else {
                    onFailure("User not found")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to get user")
            }
    }
    fun deleteRequest(
        requestId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("requests").document(requestId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to delete")
            }
    }
}