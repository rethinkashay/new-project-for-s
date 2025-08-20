package de.ashaysurya.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DashboardRepository

    val todaysRevenue: LiveData<Double?>
    val todaysOrderCount: LiveData<Int?>
    val weeklySalesSummary: LiveData<List<DailySalesSummary>>

    // LiveData to hold the report data when it's ready
    private val _salesReportData = MutableLiveData<List<SalesReportItem>>()
    val salesReportData: LiveData<List<SalesReportItem>> = _salesReportData

    init {
        val orderDao = AppDatabase.getDatabase(application).orderDao()
        repository = DashboardRepository(orderDao)
        todaysRevenue = repository.todaysRevenue
        todaysOrderCount = repository.todaysOrderCount
        weeklySalesSummary = repository.weeklySalesSummary
    }

    /**
     * Called by the Activity to start the data export process.
     * It fetches the data from the repository and posts it to the LiveData.
     */
    fun prepareDataForExport() {
        viewModelScope.launch {
            _salesReportData.postValue(repository.getSalesReportData())
        }
    }
}