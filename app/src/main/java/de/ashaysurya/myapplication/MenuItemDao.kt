package de.ashaysurya.myapplication

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * @Dao tells Room that this is a Data Access Object.
 * This is where we define all our database interactions.
 */
@Dao
interface MenuItemDao {

    /**
     * @Insert handles inserting a new item. onConflict means if we try to insert an item
     * with an ID that already exists, we should just ignore the new one.
     * 'suspend' means this function can be paused and resumed, making it safe to call
     * from a background thread (using Coroutines) without freezing the app.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(menuItem: MenuItem)

    @Update
    suspend fun update(menuItem: MenuItem)

    @Delete
    suspend fun delete(menuItem: MenuItem)

    /**
     * @Query allows us to write our own SQL queries. This one gets all menu items
     * and orders them by name.
     * It returns LiveData, so our UI can observe it for any changes automatically.
     */
    @Query("SELECT * FROM menu_items ORDER BY name ASC")
    fun getAllMenuItems(): LiveData<List<MenuItem>>
}