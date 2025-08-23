// File: DashboardRepository.kt
package de.ashaysurya.myapplication

import androidx.lifecycle.LiveData
import java.util.Calendar
import java.util.Date

class DashboardRepository(private val orderDao: OrderDao) {

    private val todayStartMillis: Long
    private val todayEndMillis: Long
    private val sevenDaysAgoStartMillis: Long

    init {
        val calendar = Calendar.getInstance()
        // Set to the end of today (23:59:59)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        todayEndMillis = calendar.timeInMillis

        // Set to the beginning of today (00:00:00)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        todayStartMillis = calendar.timeInMillis

        // Set to the beginning of the day 6 days ago (for a 7-day rolling period)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        sevenDaysAgoStartMillis = calendar.timeInMillis
    }

    val todaysRevenue: LiveData<Double?> = orderDao.getRevenueForDay(todayStartMillis, todayEndMillis)
    val todaysOrderCount: LiveData<Int?> = orderDao.getOrderCountForDay(todayStartMillis, todayEndMillis)
    val weeklySalesSummary: LiveData<List<DailySalesSummary>> = orderDao.getWeeklySalesSummary()

    // NEW: Function to get payment split for today
    suspend fun getTodaysPaymentSplit(): List<PaymentMethodTotal> {
        // We use Date().time for todayEndMillis to capture sales up to the current moment
        return orderDao.getTotalsByPaymentMethod(todayStartMillis, Date().time)
    }

    // NEW: Function to get payment split for the last 7 days
    suspend fun getWeeklyPaymentSplit(): List<PaymentMethodTotal> {
        return orderDao.getTotalsByPaymentMethod(sevenDaysAgoStartMillis, Date().time)
    }

    suspend fun getSalesReportData(): List<SalesReportItem> {
        return orderDao.getSalesReportData()
    }
}