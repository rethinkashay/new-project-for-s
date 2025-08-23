// File: DashboardActivity.kt
package de.ashaysurya.myapplication

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()

    // References to the new TextViews for payment split
    private lateinit var tvTodayCash: TextView
    private lateinit var tvTodayCredit: TextView
    private lateinit var tvTodayUpi: TextView
    private lateinit var tvWeeklyCash: TextView
    private lateinit var tvWeeklyCredit: TextView
    private lateinit var tvWeeklyUpi: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val revenueTextView = findViewById<TextView>(R.id.textViewTodaysRevenue)
        val ordersTextView = findViewById<TextView>(R.id.textViewTodaysOrders)
        val exportButton = findViewById<Button>(R.id.buttonExport)
        val summaryRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewWeeklySummary)

        // Initialize new TextViews
        tvTodayCash = findViewById(R.id.textViewTodayCash)
        tvTodayCredit = findViewById(R.id.textViewTodayCredit)
        tvTodayUpi = findViewById(R.id.textViewTodayUpi)
        tvWeeklyCash = findViewById(R.id.textViewWeeklyCash)
        tvWeeklyCredit = findViewById(R.id.textViewWeeklyCredit)
        tvWeeklyUpi = findViewById(R.id.textViewWeeklyUpi)

        val summaryAdapter = SummaryListAdapter()
        summaryRecyclerView.adapter = summaryAdapter
        summaryRecyclerView.layoutManager = LinearLayoutManager(this)

        // Observe existing LiveData
        dashboardViewModel.todaysRevenue.observe(this) { revenue ->
            revenueTextView.text = String.format("₹%.2f", revenue ?: 0.0)
        }

        dashboardViewModel.todaysOrderCount.observe(this) { count ->
            ordersTextView.text = (count ?: 0).toString()
        }

        dashboardViewModel.weeklySalesSummary.observe(this) { summaryList ->
            summaryAdapter.submitList(summaryList)
        }

        // NEW: Observe payment split LiveData
        dashboardViewModel.todaysPaymentSplit.observe(this) { split ->
            updatePaymentSplitUI(split, isToday = true)
        }
        dashboardViewModel.weeklyPaymentSplit.observe(this) { split ->
            updatePaymentSplitUI(split, isToday = false)
        }

        exportButton.setOnClickListener {
            dashboardViewModel.prepareDataForExport()
            Toast.makeText(this, "Preparing report...", Toast.LENGTH_SHORT).show()
        }

        // Observe the new FullReportData
        dashboardViewModel.fullReportData.observe(this) { reportData ->
            if (reportData.detailedSales.isNotEmpty()) {
                createAndSaveExcelFile(reportData)
            }
        }
    }

    // Helper function to update the UI
    private fun updatePaymentSplitUI(split: List<PaymentMethodTotal>, isToday: Boolean) {
        val defaultText = "₹0.00"
        var cashTotal = defaultText
        var creditTotal = defaultText
        var upiTotal = defaultText

        split.forEach {
            val formattedTotal = String.format("₹%.2f", it.total)
            when (it.paymentMethod) {
                PaymentMethod.CASH -> cashTotal = formattedTotal
                PaymentMethod.CREDIT -> creditTotal = formattedTotal
                PaymentMethod.UPI -> upiTotal = formattedTotal
            }
        }

        if (isToday) {
            tvTodayCash.text = cashTotal
            tvTodayCredit.text = creditTotal
            tvTodayUpi.text = upiTotal
        } else {
            tvWeeklyCash.text = cashTotal
            tvWeeklyCredit.text = creditTotal
            tvWeeklyUpi.text = upiTotal
        }
    }

    // UPDATED: This function is now completely overhauled for the new report
    private fun createAndSaveExcelFile(fullReportData: FullReportData) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val workbook = XSSFWorkbook()

                // --- Sheet 1: Detailed Sales Report ---
                val detailedSheet = workbook.createSheet("Sales Report")
                val headerRow = detailedSheet.createRow(0)
                headerRow.createCell(0).setCellValue("Order ID")
                headerRow.createCell(1).setCellValue("Date")
                headerRow.createCell(2).setCellValue("Time")
                headerRow.createCell(3).setCellValue("Payment Method") // New Column
                headerRow.createCell(4).setCellValue("Item Name")
                headerRow.createCell(5).setCellValue("Quantity")
                headerRow.createCell(6).setCellValue("Price Per Item")
                headerRow.createCell(7).setCellValue("Line Total")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                fullReportData.detailedSales.forEachIndexed { index, item ->
                    val row = detailedSheet.createRow(index + 1)
                    row.createCell(0).setCellValue(item.orderId.toDouble())
                    row.createCell(1).setCellValue(dateFormat.format(item.timestamp))
                    row.createCell(2).setCellValue(timeFormat.format(item.timestamp))
                    row.createCell(3).setCellValue(item.paymentMethod.name) // New Data
                    row.createCell(4).setCellValue(item.itemName)
                    row.createCell(5).setCellValue(item.quantity.toDouble())
                    row.createCell(6).setCellValue(item.pricePerItem)
                    row.createCell(7).setCellValue(item.pricePerItem * item.quantity)
                }

                // --- Sheet 2: Summary Report ---
                val summarySheet = workbook.createSheet("Summary")
                val summaryHeaderRow = summarySheet.createRow(0)
                summaryHeaderRow.createCell(0).setCellValue("Period")
                summaryHeaderRow.createCell(1).setCellValue("Payment Method")
                summaryHeaderRow.createCell(2).setCellValue("Total Sales")

                var currentRow = 1
                // Add Today's summary
                summarySheet.createRow(currentRow++).createCell(0).setCellValue("Today's Summary")
                fullReportData.todaysSplit.forEach {
                    val row = summarySheet.createRow(currentRow++)
                    row.createCell(1).setCellValue(it.paymentMethod.name)
                    row.createCell(2).setCellValue(String.format("₹%.2f", it.total))
                }

                // Add a blank row for spacing
                currentRow++

                // Add Weekly summary
                summarySheet.createRow(currentRow++).createCell(0).setCellValue("Last 7 Days Summary")
                fullReportData.weeklySplit.forEach {
                    val row = summarySheet.createRow(currentRow++)
                    row.createCell(1).setCellValue(it.paymentMethod.name)
                    row.createCell(2).setCellValue(String.format("₹%.2f", it.total))
                }

                // --- Save the file ---
                val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val fileName = "SalesReport_$dateStamp.xlsx"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { stream ->
                        workbook.write(stream)
                    }
                }
                workbook.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardActivity, "Report saved to Downloads folder!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardActivity, "Error creating report: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}