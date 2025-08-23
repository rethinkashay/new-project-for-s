// File: DashboardViewModel.kt
package de.ashaysurya.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// NEW: A data class to hold all data for the Excel export
data class FullReportData(
    val detailedSales: List<SalesReportItem>,
    val todaysSplit: List<PaymentMethodTotal>,
    val weeklySplit: List<PaymentMethodTotal>
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DashboardRepository

    val todaysRevenue: LiveData<Double?>
    val todaysOrderCount: LiveData<Int?>
    val weeklySalesSummary: LiveData<List<DailySalesSummary>>

    // NEW: LiveData for the payment splits
    private val _todaysPaymentSplit = MutableLiveData<List<PaymentMethodTotal>>()
    val todaysPaymentSplit: LiveData<List<PaymentMethodTotal>> = _todaysPaymentSplit

    private val _weeklyPaymentSplit = MutableLiveData<List<PaymentMethodTotal>>()
    val weeklyPaymentSplit: LiveData<List<PaymentMethodTotal>> = _weeklyPaymentSplit

    // This now holds our new, richer data structure
    private val _fullReportData = MutableLiveData<FullReportData>()
    val fullReportData: LiveData<FullReportData> = _fullReportData

    init {
        val orderDao = AppDatabase.getDatabase(application).orderDao()
        repository = DashboardRepository(orderDao)
        todaysRevenue = repository.todaysRevenue
        todaysOrderCount = repository.todaysOrderCount
        weeklySalesSummary = repository.weeklySalesSummary

        // Load the payment split data when the ViewModel is created
        loadPaymentSplits()
    }

    private fun loadPaymentSplits() {
        viewModelScope.launch {
            _todaysPaymentSplit.postValue(repository.getTodaysPaymentSplit())
            _weeklyPaymentSplit.postValue(repository.getWeeklyPaymentSplit())
        }
    }

    fun prepareDataForExport() {
        viewModelScope.launch {
            // Fetch all data needed for the report
            val detailedSales = repository.getSalesReportData()
            val todaysSplit = repository.getTodaysPaymentSplit()
            val weeklySplit = repository.getWeeklyPaymentSplit()

            _fullReportData.postValue(FullReportData(detailedSales, todaysSplit, weeklySplit))
        }
    }
}