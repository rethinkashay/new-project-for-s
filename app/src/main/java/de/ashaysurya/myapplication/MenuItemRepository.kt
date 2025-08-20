package de.ashaysurya.myapplication

import androidx.lifecycle.LiveData

/**
 * The Repository is the single source of truth for our app's data.
 * It abstracts the data sources (like our Room database) from the rest of the app.
 * The ViewModel will interact with this Repository, not directly with the DAO.
 *
 * @param menuItemDao The Data Access Object for menu items.
 */
class MenuItemRepository(private val menuItemDao: MenuItemDao) {

    // This property holds all the menu items from the database as LiveData.
    // The DAO provides this, and the Repository makes it available to the ViewModel.
    val allMenuItems: LiveData<List<MenuItem>> = menuItemDao.getAllMenuItems()

    /**
     * A non-blocking function to insert a new menu item into the database.
     * We use 'suspend' to indicate that this should be called from a coroutine.
     */
    suspend fun insert(menuItem: MenuItem) {
        menuItemDao.insert(menuItem)
    }

    /**
     * A non-blocking function to update an existing menu item.
     */
    suspend fun update(menuItem: MenuItem) {
        menuItemDao.update(menuItem)
    }

    /**
     * A non-blocking function to delete a menu item.
     */
    suspend fun delete(menuItem: MenuItem) {
        menuItemDao.delete(menuItem)
    }
}