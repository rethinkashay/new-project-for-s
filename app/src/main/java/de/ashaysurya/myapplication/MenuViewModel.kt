package de.ashaysurya.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The ViewModel provides data to the UI and survives configuration changes.
 * It acts as a communication center between the Repository and the UI.
 *
 * We use AndroidViewModel because we need the application context to initialize the database.
 */
class MenuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MenuItemRepository

    // Using LiveData and caching what getAllMenuItems returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allMenuItems: LiveData<List<MenuItem>>

    init {
        // Get a reference to the DAO from the database instance
        val menuItemDao = AppDatabase.getDatabase(application).menuItemDao()
        // Initialize the repository with the DAO
        repository = MenuItemRepository(menuItemDao)
        // Get all menu items from the repository
        allMenuItems = repository.allMenuItems
    }

    /**
     * Launch a new coroutine to insert the data in a non-blocking way
     */
    fun insert(menuItem: MenuItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(menuItem)
    }

    /**
     * Launch a new coroutine to update the data in a non-blocking way
     */
    fun update(menuItem: MenuItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(menuItem)
    }

    /**
     * Launch a new coroutine to delete the data in a non-blocking way
     */
    fun delete(menuItem: MenuItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(menuItem)
    }
}