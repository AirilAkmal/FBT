package com.example.fbt2

data class Request(
    val id: Int = 0,
    val userIdNumber: String = "",
    val userName: String = "",
    val category: String = "",
    val item: String = "",
    val purpose: String = "",
    val borrowingDate: String = "",
    val returningDate: String = "",
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
)