// File: AppDatabase.kt
package de.ashaysurya.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 1. Change version from 2 to 3
@Database(entities = [MenuItem::class, Order::class, OrderItem::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun menuItemDao(): MenuItemDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 2. Define the Migration object to safely add the new column
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE orders ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restaurant_database"
                )
                    // 3. REMOVE .fallbackToDestructiveMigration()
                    // 4. ADD the new safe migration instead
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}