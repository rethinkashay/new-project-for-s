package de.ashaysurya.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PosActivity : AppCompatActivity() {

    private val posViewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos)

        // --- Setup for Menu Grid (Left Side) ---
        val menuRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewMenu)
        val menuAdapter = MenuGridAdapter { menuItem ->
            // When a menu item is clicked, add it to the order
            posViewModel.addItemToOrder(menuItem)
        }
        menuRecyclerView.adapter = menuAdapter
        // Use a GridLayoutManager to show items in a grid
        menuRecyclerView.layoutManager = GridLayoutManager(this, 3)

        // --- Setup for Order List (Right Side) ---
        val orderRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewOrder)
        val orderAdapter = OrderListAdapter()
        orderRecyclerView.adapter = orderAdapter
        orderRecyclerView.layoutManager = LinearLayoutManager(this)

        val totalTextView = findViewById<TextView>(R.id.textViewTotal)
        val payButton = findViewById<Button>(R.id.buttonPay)

        // --- Observe LiveData from the ViewModel ---

        // Observe the list of all menu items
        posViewModel.allMenuItems.observe(this) { items ->
            items?.let { menuAdapter.submitList(it) }
        }

        // Observe the current order to update the order list
        posViewModel.currentOrder.observe(this) { order ->
            order?.let {
                // Convert the map to a list of pairs for the adapter
                orderAdapter.submitList(it.toList())
            }
        }

        // Observe the total amount to update the total text view
        posViewModel.totalAmount.observe(this) { total ->
            totalTextView.text = String.format("₹%.2f", total)
        }

        // --- Setup for the Pay Button ---
        payButton.setOnClickListener {
            posViewModel.finalizeSale()
            Toast.makeText(this, "Sale Finalized!", Toast.LENGTH_SHORT).show()
        }
    }
}