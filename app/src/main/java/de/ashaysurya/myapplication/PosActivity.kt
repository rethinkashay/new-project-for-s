// File: PosActivity.kt
package de.ashaysurya.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import de.ashaysurya.myapplication.databinding.ActivityPosBinding


private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

// We've temporarily removed the PaymentMethodSelectionListener for now
class PosActivity : AppCompatActivity() {

    private val posViewModel: PosViewModel by viewModels()
    private lateinit var binding: ActivityPosBinding

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                posViewModel.setSearchQuery(s.toString())
            }
        })
    }
    // We need to define these constants here for the GridLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // --- Setup Adapters ---
        val menuAdapter = MenuGridAdapter { menuItem ->
            posViewModel.addItemToOrder(menuItem)
        }
        val orderAdapter = OrderListAdapter()

        // --- Setup RecyclerViews ---
        setupMenuRecyclerView(menuAdapter)
        binding.recyclerViewOrder.adapter = orderAdapter
        binding.recyclerViewOrder.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)


        // --- Observe LiveData from the ViewModel ---
        observeViewModel(menuAdapter, orderAdapter)

        // --- Setup for Click Listeners ---
        setupClickListeners()
    }

    private fun observeViewModel(menuAdapter: MenuGridAdapter, orderAdapter: OrderListAdapter) {
        // Observe the new grouped list
        posViewModel.groupedMenuItems.observe(this) { items ->
            items?.let { menuAdapter.submitList(it) }
        }

        // The order list observation remains the same
        posViewModel.currentOrder.observe(this) { order ->
            order?.let {
                orderAdapter.submitList(it.toList())
                binding.buttonFinalizeSale.isEnabled = it.isNotEmpty()
            }
        }

        // We have temporarily removed the observers for totalAmount and selectedPaymentMethod
    }

    private fun setupMenuRecyclerView(menuAdapter: MenuGridAdapter) {
        val layoutManager = GridLayoutManager(this, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (menuAdapter.getItemViewType(position)) {
                    ITEM_VIEW_TYPE_HEADER -> 2
                    ITEM_VIEW_TYPE_ITEM -> 1
                    else -> 1
                }
            }
        }
        binding.recyclerViewMenu.adapter = menuAdapter
        binding.recyclerViewMenu.layoutManager = layoutManager
    }

    private fun setupClickListeners()  {
        setupSearch()
        binding.buttonFinalizeSale.setOnClickListener {
            // The logic for finalizeSale might change later, but this is fine for now
            val success = posViewModel.finalizeSale()
            if (success) {
                Toast.makeText(this, "Sale Finalized!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Could not finalize sale.", Toast.LENGTH_SHORT).show()
            }
        }
        // We have temporarily removed the click listener for the payment button
    }

    // We have temporarily removed the onPaymentMethodSelected and updatePaymentMethodButton functions
}