// File: PosViewModel.kt
package de.ashaysurya.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// This sealed class remains the same
sealed class DataItem {
    data class MenuItemWrapper(val menuItem: MenuItem) : DataItem()
    data class HeaderWrapper(val categoryName: String) : DataItem()
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OrderRepository
    private val allMenuItems: LiveData<List<MenuItem>>

    // NEW: LiveData to hold the current search query from the user.
    private val searchQuery = MutableLiveData("")

    // This is now a MediatorLiveData to combine allMenuItems and searchQuery.
    val groupedMenuItems = MediatorLiveData<List<DataItem>>()

    private val _currentOrder = MutableLiveData<MutableMap<MenuItem, Int>>(mutableMapOf())
    val currentOrder: LiveData<MutableMap<MenuItem, Int>> = _currentOrder

    private val _totalAmount = MutableLiveData(0.0)
    val totalAmount: LiveData<Double> = _totalAmount

    private val _selectedPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedPaymentMethod: StateFlow<PaymentMethod?> = _selectedPaymentMethod.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = OrderRepository(database.menuItemDao(), database.orderDao())
        allMenuItems = repository.allMenuItems
        setPaymentMethod(PaymentMethod.CASH)

        // Tell the MediatorLiveData which sources to observe.
        groupedMenuItems.addSource(allMenuItems) { updateGroupedList() }
        groupedMenuItems.addSource(searchQuery) { updateGroupedList() }
    }

    // NEW: This function is called whenever the menu list or search query changes.
    private fun updateGroupedList() {
        val items = allMenuItems.value ?: return
        val query = searchQuery.value ?: ""

        // 1. Filter the list based on the search query.
        val filteredItems = if (query.isEmpty()) {
            items
        } else {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }

        // 2. Group the *filtered* list by category.
        val groupedList = mutableListOf<DataItem>()
        val itemsByCategory = filteredItems.groupBy { it.category }

        for ((category, menuItems) in itemsByCategory) {
            groupedList.add(DataItem.HeaderWrapper(category))
            menuItems.forEach { menuItem ->
                groupedList.add(DataItem.MenuItemWrapper(menuItem))
            }
        }
        groupedMenuItems.value = groupedList
    }

    // NEW: Function for the Activity to call when the search text changes.
    fun setSearchQuery(query: String) {
        searchQuery.value = query
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

    fun setPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun finalizeSale(): Boolean {
        val paymentMethod = _selectedPaymentMethod.value
        val currentOrderItems = _currentOrder.value

        if (paymentMethod == null || currentOrderItems.isNullOrEmpty()) {
            return false
        }

        viewModelScope.launch {
            repository.insertOrder(currentOrderItems, paymentMethod)
            _currentOrder.postValue(mutableMapOf())
            _totalAmount.postValue(0.0)
        }
        return true
    }
}