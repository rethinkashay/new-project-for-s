// File: PosActivity.kt
package de.ashaysurya.myapplication

import android.os.Bundle
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import de.ashaysurya.myapplication.databinding.ActivityPosBinding
import kotlinx.coroutines.launch

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

class PosActivity : AppCompatActivity(), PaymentMethodSelectorFragment.PaymentMethodSelectionListener {

    private val posViewModel: PosViewModel by viewModels()
    private lateinit var binding: ActivityPosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Setup Adapters ---
        val menuAdapter = MenuGridAdapter { menuItem ->
            posViewModel.addItemToOrder(menuItem)
        }
        // UPDATED: Pass the new functions to the OrderListAdapter constructor
        val orderAdapter = OrderListAdapter(
            onIncrement = { menuItem -> posViewModel.incrementItemQuantity(menuItem) },
            onDecrement = { menuItem -> posViewModel.decrementItemQuantity(menuItem) }
        )

        // --- Setup RecyclerViews ---
        setupMenuRecyclerView(menuAdapter)
        binding.recyclerViewOrder.adapter = orderAdapter
        binding.recyclerViewOrder.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        // --- Observe LiveData from the ViewModel ---
        observeViewModel(menuAdapter, orderAdapter)

        // --- Setup for Click Listeners and Search ---
        setupClickListeners()
        setupSearch()
    }

    override fun onPaymentMethodSelected(method: PaymentMethod) {
        posViewModel.setPaymentMethod(method)
    }

    private fun observeViewModel(menuAdapter: MenuGridAdapter, orderAdapter: OrderListAdapter) {
        posViewModel.groupedMenuItems.observe(this) { items ->
            items?.let { menuAdapter.submitList(it) }
        }

        posViewModel.currentOrder.observe(this) { order ->
            order?.let {
                orderAdapter.submitList(it.toList())
                binding.buttonFinalizeSale.isEnabled = it.isNotEmpty()
            }
        }

        // ADDED BACK: Observe total and payment method
        posViewModel.totalAmount.observe(this) { total ->
            binding.textViewTotal.text = String.format("₹%.2f", total)
        }

        lifecycleScope.launch {
            posViewModel.selectedPaymentMethod.collect { method ->
                updatePaymentMethodButton(method)
            }
        }
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

    private fun setupClickListeners() {
        // ADDED BACK: The payment selector click listener
        binding.buttonSelectPayment.setOnClickListener {
            PaymentMethodSelectorFragment.newInstance()
                .show(supportFragmentManager, PaymentMethodSelectorFragment.TAG)
        }

        binding.buttonFinalizeSale.setOnClickListener {
            val success = posViewModel.finalizeSale()
            if (success) {
                Toast.makeText(this, "Sale Finalized!", Toast.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Please add items and select a payment method.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                posViewModel.setSearchQuery(s.toString())
            }
        })
    }

    // ADDED BACK: The helper function to update the payment button UI
    private fun updatePaymentMethodButton(method: PaymentMethod?) {
        when (method) {
            PaymentMethod.CASH -> {
                binding.buttonSelectPayment.text = "Cash"
                binding.buttonSelectPayment.setIconResource(R.drawable.ic_cash)
            }
            PaymentMethod.CREDIT -> {
                binding.buttonSelectPayment.text = "Credit Card"
                binding.buttonSelectPayment.setIconResource(R.drawable.ic_credit_card)
            }
            PaymentMethod.UPI -> {
                binding.buttonSelectPayment.text = "UPI"
                binding.buttonSelectPayment.setIconResource(R.drawable.ic_upi)
            }
            null -> {
                binding.buttonSelectPayment.text = "Select Payment Method"
                binding.buttonSelectPayment.icon = null
            }
        }
    }
}