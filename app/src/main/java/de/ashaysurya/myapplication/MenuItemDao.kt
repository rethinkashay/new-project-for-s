package de.ashaysurya.myapplication

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MenuItemDao {

    /**
     * UPDATED: Changed the conflict strategy to REPLACE.
     * This is more robust and ensures the item is always saved.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(menuItem: MenuItem)

    /**
     * UPDATED: Added the REPLACE strategy for updates as well.
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(menuItem: MenuItem)

    @Delete
    suspend fun delete(menuItem: MenuItem)

    @Query("SELECT * FROM menu_items ORDER BY name ASC")
    fun getAllMenuItems(): LiveData<List<MenuItem>>
}
