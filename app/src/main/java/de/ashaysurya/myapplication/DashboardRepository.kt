package de.ashaysurya.myapplication

import androidx.lifecycle.LiveData
import java.util.Calendar

/**
 * Repository for fetching dashboard-related data from the OrderDao.
 */
class DashboardRepository(private val orderDao: OrderDao) {

    // Get the start and end of the current day in milliseconds
    private val todayStartMillis: Long
    private val todayEndMillis: Long

    init {
        val calendar = Calendar.getInstance()
        // Set to the beginning of today (00:00:00)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        todayStartMillis = calendar.timeInMillis

        // Set to the end of today (23:59:59)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        todayEndMillis = calendar.timeInMillis
    }

    // LiveData for today's total revenue
    val todaysRevenue: LiveData<Double?> = orderDao.getRevenueForDay(todayStartMillis, todayEndMillis)

    // LiveData for today's total order count
    val todaysOrderCount: LiveData<Int?> = orderDao.getOrderCountForDay(todayStartMillis, todayEndMillis)

    // LiveData for the weekly sales summary
    val weeklySalesSummary: LiveData<List<DailySalesSummary>> = orderDao.getWeeklySalesSummary()

    // NEW FUNCTION: A one-time fetch for all sales report data
    suspend fun getSalesReportData(): List<SalesReportItem> {
        return orderDao.getSalesReportData()
    }
} // <-- The closing brace for the class goes here, at the very end.