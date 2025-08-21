// File: Order.kt
package de.ashaysurya.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val orderId: Long = 0,
    val timestamp: Date,
    val totalAmount: Double,

    // Add this new column.
    // The defaultValue is important for the migration to work on existing rows.
    @ColumnInfo(name = "paymentMethod", defaultValue = "CASH")
    val paymentMethod: PaymentMethod
)