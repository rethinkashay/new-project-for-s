// File: MenuItem.kt
package de.ashaysurya.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val price: Double,

    // We are keeping the category field
    @ColumnInfo(defaultValue = "Default")
    val category: String,

    // The 'stock' property has been removed.
)