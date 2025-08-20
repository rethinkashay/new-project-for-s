package de.ashaysurya.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

// The @Entity annotation tells Room that this class represents a table in our database.
@Entity(tableName = "menu_items")
data class MenuItem(
    // @PrimaryKey tells Room this is the unique ID for each row.
    // autoGenerate = true means Room will create a new ID for each item we add.
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // These are the other columns in our table.
    val name: String,
    val price: Double
)