package de.ashaysurya.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OrderRepository

    // LiveData holding all menu items from the database.
    val allMenuItems: LiveData<List<MenuItem>>

    // LiveData holding the items in the current order (MenuItem to Quantity).
    private val _currentOrder = MutableLiveData<MutableMap<MenuItem, Int>>(mutableMapOf())
    val currentOrder: LiveData<MutableMap<MenuItem, Int>> = _currentOrder

    // LiveData holding the calculated total of the current order.
    private val _totalAmount = MutableLiveData(0.0)
    val totalAmount: LiveData<Double> = _totalAmount

    init {
        val database = AppDatabase.getDatabase(application)
        repository = OrderRepository(database.menuItemDao(), database.orderDao())
        allMenuItems = repository.allMenuItems
    }

    /**
     * Adds a selected menu item to the current order or increments its quantity.
     */
    fun addItemToOrder(menuItem: MenuItem) {
        val order = _currentOrder.value ?: mutableMapOf()
        val quantity = order[menuItem] ?: 0
        order[menuItem] = quantity + 1
        _currentOrder.value = order
        calculateTotal()
    }

    /**
     * Calculates the total amount of the current order and updates the LiveData.
     */
    private fun calculateTotal() {
        val order = _currentOrder.value ?: return
        val total = order.entries.sumOf { (item, quantity) -> item.price * quantity }
        _totalAmount.value = total
    }

    /**
     * Finalizes the sale by saving the order to the database.
     */
    fun finalizeSale() {
        viewModelScope.launch {
            _currentOrder.value?.let {
                if (it.isNotEmpty()) {
                    repository.insertOrder(it)
                    // Clear the current order after saving
                    _currentOrder.postValue(mutableMapOf())
                    _totalAmount.postValue(0.0)
                }
            }
        }
    }
}