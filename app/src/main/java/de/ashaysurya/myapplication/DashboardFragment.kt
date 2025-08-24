package de.ashaysurya.myapplication

import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.ashaysurya.myapplication.databinding.FragmentDashboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val summaryAdapter = SummaryListAdapter()
        binding.recyclerViewWeeklySummary.adapter = summaryAdapter
        binding.recyclerViewWeeklySummary.layoutManager = LinearLayoutManager(requireContext())

        setupObservers(summaryAdapter)
        setupClickListeners()
    }

    private fun setupObservers(summaryAdapter: SummaryListAdapter) {
        dashboardViewModel.todaysRevenue.observe(viewLifecycleOwner) { revenue ->
            binding.textViewTodaysRevenue.text = String.format("₹%.2f", revenue ?: 0.0)
        }

        dashboardViewModel.todaysOrderCount.observe(viewLifecycleOwner) { count ->
            binding.textViewTodaysOrders.text = (count ?: 0).toString()
        }

        dashboardViewModel.weeklySalesSummary.observe(viewLifecycleOwner) { summaryList ->
            summaryAdapter.submitList(summaryList)
        }

        dashboardViewModel.todaysPaymentSplit.observe(viewLifecycleOwner) { split ->
            updatePaymentSplitUI(split, isToday = true)
        }
        dashboardViewModel.weeklyPaymentSplit.observe(viewLifecycleOwner) { split ->
            updatePaymentSplitUI(split, isToday = false)
        }

        dashboardViewModel.fullReportData.observe(viewLifecycleOwner) { reportData ->
            if (reportData.detailedSales.isNotEmpty()) {
                createAndSaveExcelFile(reportData)
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonExport.setOnClickListener {
            dashboardViewModel.prepareDataForExport()
            Toast.makeText(requireContext(), "Preparing report...", Toast.LENGTH_SHORT).show()
        }
    }

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
            binding.textViewTodayCash.text = cashTotal
            binding.textViewTodayCredit.text = creditTotal
            binding.textViewTodayUpi.text = upiTotal
        } else {
            binding.textViewWeeklyCash.text = cashTotal
            binding.textViewWeeklyCredit.text = creditTotal
            binding.textViewWeeklyUpi.text = upiTotal
        }
    }

    private fun createAndSaveExcelFile(fullReportData: FullReportData) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val workbook = XSSFWorkbook()

                val detailedSheet = workbook.createSheet("Sales Report")
                val headerRow = detailedSheet.createRow(0)
                headerRow.createCell(0).setCellValue("Order ID")
                headerRow.createCell(1).setCellValue("Date")
                headerRow.createCell(2).setCellValue("Time")
                headerRow.createCell(3).setCellValue("Payment Method")
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
                    row.createCell(3).setCellValue(item.paymentMethod.name)
                    row.createCell(4).setCellValue(item.itemName)
                    row.createCell(5).setCellValue(item.quantity.toDouble())
                    row.createCell(6).setCellValue(item.pricePerItem)
                    row.createCell(7).setCellValue(item.pricePerItem * item.quantity)
                }

                val summarySheet = workbook.createSheet("Summary")
                val summaryHeaderRow = summarySheet.createRow(0)
                summaryHeaderRow.createCell(0).setCellValue("Period")
                summaryHeaderRow.createCell(1).setCellValue("Payment Method")
                summaryHeaderRow.createCell(2).setCellValue("Total Sales")

                var currentRow = 1
                summarySheet.createRow(currentRow++).createCell(0).setCellValue("Today's Summary")
                fullReportData.todaysSplit.forEach {
                    val row = summarySheet.createRow(currentRow++)
                    row.createCell(1).setCellValue(it.paymentMethod.name)
                    row.createCell(2).setCellValue(String.format("₹%.2f", it.total))
                }

                currentRow++

                summarySheet.createRow(currentRow++).createCell(0).setCellValue("Last 7 Days Summary")
                fullReportData.weeklySplit.forEach {
                    val row = summarySheet.createRow(currentRow++)
                    row.createCell(1).setCellValue(it.paymentMethod.name)
                    row.createCell(2).setCellValue(String.format("₹%.2f", it.total))
                }

                val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val fileName = "SalesReport_$dateStamp.xlsx"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = requireActivity().contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { stream ->
                        workbook.write(stream)
                    }
                }
                workbook.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Report saved to Downloads folder!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error creating report: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}