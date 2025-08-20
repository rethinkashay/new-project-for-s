package de.ashaysurya.myapplication

import androidx.room.Entity

// This makes a composite primary key from orderId and menuItemId
@Entity(tableName = "order_items", primaryKeys = ["orderId", "menuItemId"])
data class OrderItem(
    val orderId: Long,
    val menuItemId: Int,
    val quantity: Int,
    val price: Double // Price of the item at the time of sale
)