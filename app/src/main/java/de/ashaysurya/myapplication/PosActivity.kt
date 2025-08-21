// File: PosActivity.kt
package de.ashaysurya.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import de.ashaysurya.myapplication.databinding.ActivityPosBinding
import kotlinx.coroutines.launch

// 1. Implement the listener interface from our fragment
class PosActivity : AppCompatActivity(), PaymentMethodSelectorFragment.PaymentMethodSelectionListener {

    private val posViewModel: PosViewModel by viewModels()
    // 2. Set up ViewBinding
    private lateinit var binding: ActivityPosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 3. Inflate the layout using ViewBinding
        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Setup for Adapters ---
        val menuAdapter = MenuGridAdapter { menuItem ->
            posViewModel.addItemToOrder(menuItem)
        }
        val orderAdapter = OrderListAdapter()

        // --- Setup RecyclerViews using ViewBinding ---
        binding.recyclerViewMenu.adapter = menuAdapter
        binding.recyclerViewMenu.layoutManager = GridLayoutManager(this, 3)

        binding.recyclerViewOrder.adapter = orderAdapter
        binding.recyclerViewOrder.layoutManager = LinearLayoutManager(this)

        // --- Observe LiveData from the ViewModel ---
        observeViewModel(menuAdapter, orderAdapter)

        // --- Setup for Click Listeners ---
        setupClickListeners()
    }

    // 4. Implement the required method from the interface
    override fun onPaymentMethodSelected(method: PaymentMethod) {
        posViewModel.setPaymentMethod(method)
    }

    private fun observeViewModel(menuAdapter: MenuGridAdapter, orderAdapter: OrderListAdapter) {
        posViewModel.allMenuItems.observe(this) { items ->
            items?.let { menuAdapter.submitList(it) }
        }

        posViewModel.currentOrder.observe(this) { order ->
            order?.let {
                orderAdapter.submitList(it.toList())
                // Enable/disable the finalize button based on whether the cart has items
                binding.buttonFinalizeSale.isEnabled = it.isNotEmpty()
            }
        }

        posViewModel.totalAmount.observe(this) { total ->
            binding.textViewTotal.text = String.format("₹%.2f", total)
        }

        // 5. Observe the selected payment method StateFlow
        lifecycleScope.launch {
            posViewModel.selectedPaymentMethod.collect { method ->
                updatePaymentMethodButton(method)
            }
        }
    }

    private fun setupClickListeners() {
        // 6. Open the bottom sheet when the payment button is clicked
        binding.buttonSelectPayment.setOnClickListener {
            PaymentMethodSelectorFragment.newInstance()
                .show(supportFragmentManager, PaymentMethodSelectorFragment.TAG)
        }

        binding.buttonFinalizeSale.setOnClickListener {
            // 7. Check the result from the ViewModel
            val success = posViewModel.finalizeSale()
            if (success) {
                Toast.makeText(this, "Sale Finalized!", Toast.LENGTH_SHORT).show()
            } else {
                // Show an error message if something went wrong (e.g., no items or no payment method)
                Snackbar.make(binding.root, "Please add items and select a payment method.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // 8. Helper function to update the button UI
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