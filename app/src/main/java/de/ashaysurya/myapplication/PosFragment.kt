package de.ashaysurya.myapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import de.ashaysurya.myapplication.databinding.FragmentPosBinding
import kotlinx.coroutines.launch

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

class PosFragment : Fragment(), PaymentMethodSelectorFragment.PaymentMethodSelectionListener {

    private val posViewModel: PosViewModel by viewModels()
    private var _binding: FragmentPosBinding? = null
    private val binding get() = _binding!!

    // Variable to hold the BottomSheetBehavior
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuAdapter = MenuGridAdapter { menuItem ->
            posViewModel.addItemToOrder(menuItem)
        }
        val orderAdapter = OrderListAdapter(
            onIncrement = { menuItem -> posViewModel.incrementItemQuantity(menuItem) },
            onDecrement = { menuItem -> posViewModel.decrementItemQuantity(menuItem) }
        )

        // --- Setup for the new Bottom Sheet ---
        // Get a reference to the behavior from the layout
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout)
        // Start with the bottom sheet hidden
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // Setup for the menu grid and order list
        setupMenuRecyclerView(menuAdapter)
        binding.recyclerViewOrder.adapter = orderAdapter
        binding.recyclerViewOrder.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        observeViewModel(menuAdapter, orderAdapter)
        setupClickListeners()
        setupSearch()
    }

    override fun onPaymentMethodSelected(method: PaymentMethod) {
        posViewModel.setPaymentMethod(method)
    }

    private fun observeViewModel(menuAdapter: MenuGridAdapter, orderAdapter: OrderListAdapter) {
        posViewModel.groupedMenuItems.observe(viewLifecycleOwner) { items ->
            items?.let { menuAdapter.submitList(it) }
        }

        // UPDATED observer for currentOrder to control the bottom sheet
        posViewModel.currentOrder.observe(viewLifecycleOwner) { orderMap ->
            orderMap?.let {
                orderAdapter.submitList(it.toList())
                binding.buttonFinalizeSale.isEnabled = it.isNotEmpty()

                if (it.isNotEmpty()) {
                    // If there are items, show the collapsed "peek" view
                    binding.textViewItemCount.text = "Items (${it.values.sum()})"
                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                } else {
                    // If the order is empty, hide the bottom sheet completely
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }

        // UPDATED observer for totalAmount to update both views
        posViewModel.totalAmount.observe(viewLifecycleOwner) { total ->
            val formattedTotal = String.format("₹%.2f", total)
            binding.textViewTotal.text = formattedTotal
            binding.textViewTotalPeek.text = formattedTotal
        }

        viewLifecycleOwner.lifecycleScope.launch {
            posViewModel.selectedPaymentMethod.collect { method ->
                updatePaymentMethodButton(method)
            }
        }
    }

    private fun setupMenuRecyclerView(menuAdapter: MenuGridAdapter) {
        val layoutManager = GridLayoutManager(requireContext(), 2)
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
        binding.buttonSelectPayment.setOnClickListener {
            PaymentMethodSelectorFragment.newInstance()
                .show(parentFragmentManager, PaymentMethodSelectorFragment.TAG)
        }

        binding.buttonFinalizeSale.setOnClickListener {
            val success = posViewModel.finalizeSale()
            if (success) {
                Toast.makeText(requireContext(), "Sale Finalized!", Toast.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Please add items and select a payment method.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                posViewModel.setSearchQuery(s.toString())
            }
        })
    }

    private fun updatePaymentMethodButton(method: PaymentMethod?) {
        val button = binding.buttonSelectPayment
        when (method) {
            PaymentMethod.CASH -> {
                button.text = "Cash"
                button.setIconResource(R.drawable.ic_cash)
            }
            PaymentMethod.CREDIT -> {
                button.text = "Credit Card"
                button.setIconResource(R.drawable.ic_credit_card)
            }
            PaymentMethod.UPI -> {
                button.text = "UPI"
                button.setIconResource(R.drawable.ic_upi)
            }
            null -> {
                button.text = "Select Payment Method"
                button.icon = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}