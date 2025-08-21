// File: PosViewModel.kt
package de.ashaysurya.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OrderRepository

    val allMenuItems: LiveData<List<MenuItem>>

    private val _currentOrder = MutableLiveData<MutableMap<MenuItem, Int>>(mutableMapOf())
    val currentOrder: LiveData<MutableMap<MenuItem, Int>> = _currentOrder

    private val _totalAmount = MutableLiveData(0.0)
    val totalAmount: LiveData<Double> = _totalAmount

    // NEW: StateFlow to hold the selected payment method. Starts as null to force a selection.
    private val _selectedPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedPaymentMethod: StateFlow<PaymentMethod?> = _selectedPaymentMethod.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = OrderRepository(database.menuItemDao(), database.orderDao())
        allMenuItems = repository.allMenuItems
        setPaymentMethod(PaymentMethod.CASH) // Set a default payment method
    }

    fun addItemToOrder(menuItem: MenuItem) {
        val order = _currentOrder.value ?: mutableMapOf()
        val quantity = order[menuItem] ?: 0
        order[menuItem] = quantity + 1
        _currentOrder.value = order
        calculateTotal()
    }

    private fun calculateTotal() {
        val order = _currentOrder.value ?: return
        val total = order.entries.sumOf { (item, quantity) -> item.price * quantity }
        _totalAmount.value = total
    }

    // NEW: Function to update the payment method from the UI.
    fun setPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    /**
     * UPDATED: Finalizes the sale using the selected payment method.
     * @return `true` if the sale was successful, `false` if no payment method was selected.
     */
    fun finalizeSale(): Boolean {
        val paymentMethod = _selectedPaymentMethod.value
        val currentOrderItems = _currentOrder.value

        // A sale cannot be finalized without a payment method or items.
        if (paymentMethod == null || currentOrderItems.isNullOrEmpty()) {
            return false
        }

        viewModelScope.launch {
            repository.insertOrder(currentOrderItems, paymentMethod)
            // Clear the current order after saving
            _currentOrder.postValue(mutableMapOf())
            _totalAmount.postValue(0.0)
        }
        return true
    }
}