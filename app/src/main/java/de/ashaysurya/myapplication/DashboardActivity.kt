package de.ashaysurya.myapplication

import android.content.ContentValues
import android.os.Build
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
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val revenueTextView = findViewById<TextView>(R.id.textViewTodaysRevenue)
        val ordersTextView = findViewById<TextView>(R.id.textViewTodaysOrders)
        val exportButton = findViewById<Button>(R.id.buttonExport)
        val summaryRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewWeeklySummary)

        val summaryAdapter = SummaryListAdapter()
        summaryRecyclerView.adapter = summaryAdapter
        summaryRecyclerView.layoutManager = LinearLayoutManager(this)

        dashboardViewModel.todaysRevenue.observe(this) { revenue ->
            revenueTextView.text = String.format("₹%.2f", revenue ?: 0.0)
        }

        dashboardViewModel.todaysOrderCount.observe(this) { count ->
            ordersTextView.text = (count ?: 0).toString()
        }

        dashboardViewModel.weeklySalesSummary.observe(this) { summaryList ->
            summaryAdapter.submitList(summaryList)
        }

        exportButton.setOnClickListener {
            dashboardViewModel.prepareDataForExport()
            Toast.makeText(this, "Preparing report...", Toast.LENGTH_SHORT).show()
        }

        dashboardViewModel.salesReportData.observe(this) { reportData ->
            // Ensure we only try to save non-empty, non-null lists
            if (!reportData.isNullOrEmpty()) {
                createAndSaveExcelFile(reportData)
            }
        }
    }

    private fun createAndSaveExcelFile(data: List<SalesReportItem>) {
        // Use lifecycleScope which is safer than GlobalScope
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Sales Report")

                // Create Header Row
                val headerRow = sheet.createRow(0)
                headerRow.createCell(0).setCellValue("Order ID")
                headerRow.createCell(1).setCellValue("Date")
                headerRow.createCell(2).setCellValue("Time")
                headerRow.createCell(3).setCellValue("Item Name")
                headerRow.createCell(4).setCellValue("Quantity")
                headerRow.createCell(5).setCellValue("Price Per Item")
                headerRow.createCell(6).setCellValue("Line Total")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                // Populate data rows
                data.forEachIndexed { index, item ->
                    val row = sheet.createRow(index + 1)
                    row.createCell(0).setCellValue(item.orderId.toDouble())
                    row.createCell(1).setCellValue(dateFormat.format(item.timestamp))
                    row.createCell(2).setCellValue(timeFormat.format(item.timestamp))
                    row.createCell(3).setCellValue(item.itemName)
                    row.createCell(4).setCellValue(item.quantity.toDouble())
                    row.createCell(5).setCellValue(item.pricePerItem)
                    row.createCell(6).setCellValue(item.pricePerItem * item.quantity)
                }

                // --- MODERN STORAGE (MediaStore API) ---
                val fileName = "SalesReport_${System.currentTimeMillis()}.xlsx"
                var outputStream: OutputStream? = null

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                }

                outputStream?.use { stream ->
                    workbook.write(stream)
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
