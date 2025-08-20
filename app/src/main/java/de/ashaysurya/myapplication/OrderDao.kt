package de.ashaysurya.myapplication

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Date

@Dao
interface OrderDao {
    // ... (existing functions) ...
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(orderItems: List<OrderItem>)

    @Query("SELECT SUM(totalAmount) FROM orders WHERE timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getRevenueForDay(startOfDay: Long, endOfDay: Long): LiveData<Double?>

    @Query("SELECT COUNT(orderId) FROM orders WHERE timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getOrderCountForDay(startOfDay: Long, endOfDay: Long): LiveData<Int?>

    @Query("SELECT date(timestamp / 1000, 'unixepoch', 'localtime') as saleDate, SUM(totalAmount) as total FROM orders GROUP BY saleDate ORDER BY saleDate DESC LIMIT 7")
    fun getWeeklySalesSummary(): LiveData<List<DailySalesSummary>>

    // NEW QUERY: Gets a detailed list of all line items from all orders for the report
    @Query("""
        SELECT o.orderId, o.timestamp, mi.name AS itemName, oi.quantity, oi.price AS pricePerItem
        FROM orders AS o
        JOIN order_items AS oi ON o.orderId = oi.orderId
        JOIN menu_items AS mi ON oi.menuItemId = mi.id
        ORDER BY o.timestamp DESC
    """)
    suspend fun getSalesReportData(): List<SalesReportItem>
}

data class DailySalesSummary(
    val saleDate: String,
    val total: Double
)

/**
 * NEW DATA CLASS: Holds the detailed information for a single row in our Excel report.
 */
data class SalesReportItem(
    val orderId: Long,
    val timestamp: Date,
    val itemName: String,
    val quantity: Int,
    val pricePerItem: Double
)