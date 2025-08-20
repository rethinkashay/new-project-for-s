package de.ashaysurya.myapplication

import android.app.Application

/**
 * A custom Application class to provide a single instance of our database and repository.
 * This ensures that these objects are created only once when the app starts.
 */
class MenuApplication : Application() {
    // Using 'lazy' means the database and repository are only created when they are first needed,
    // rather than at app startup.
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { MenuItemRepository(database.menuItemDao()) }
}