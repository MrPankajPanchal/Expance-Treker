package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("timestamp"),
        Index("dateMillis"),
        Index("type"),
        Index("category")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // "EXPENSE" or "INCOME"
    val amountCents: Long, // Integer representation of smallest currency unit (paise/cents) to prevent floating-point errors
    val category: String,
    val date: String = "", // Formatted date e.g. "2026-10-16"
    val time: String = "", // Formatted time e.g. "08:30 AM"
    val timestamp: Long = 0L, // Milliseconds since epoch
    val dateMillis: Long = 0L, // Backward-compatible timestamp alias
    val paymentMethod: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val amount: Long
        get() = amountCents
}
