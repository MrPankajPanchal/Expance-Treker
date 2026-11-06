package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name", "type"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: String, // "EXPENSE" or "INCOME"
    val iconName: String,
    val colorHex: String,
    val isCustom: Boolean = false,
    val monthlyBudgetCents: Long = 0L // 0 if no category budget set
)
