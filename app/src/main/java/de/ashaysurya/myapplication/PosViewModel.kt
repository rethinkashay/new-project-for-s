// File: PosViewModel.kt
package de.ashaysurya.myapplication
import androidx.lifecycle.map
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// NEW: A sealed class to represent the different types of items in our list.
// This allows our list to contain both Headers and MenuItems.
sealed class DataItem {
    data class MenuItemWrapper(val menuItem: MenuItem) : DataItem()
    data class HeaderWrapper(val categoryName: String) : DataItem()
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OrderRepository
    private val allMenuItems: LiveData<List<MenuItem>>

    // NEW: This LiveData will hold our transformed list with headers.
    val groupedMenuItems: LiveData<List<DataItem>>

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

        // The magic happens here! We observe the flat list from the database
        // and transform it into a grouped list with headers.
            groupedMenuItems = allMenuItems.map { items ->
            val groupedList = mutableListOf<DataItem>()
            // Group the flat list by the 'category' property.
            val itemsByCategory = items.groupBy { it.category }

            // Loop through each category and its items to build the final list.
            for ((category, menuItems) in itemsByCategory) {
                groupedList.add(DataItem.HeaderWrapper(category)) // Add the header
                menuItems.forEach { menuItem ->
                    groupedList.add(DataItem.MenuItemWrapper(menuItem)) // Add the items under that header
                }
            }
            groupedList
        }
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