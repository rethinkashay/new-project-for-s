// File: OrderRepository.kt
package de.ashaysurya.myapplication

import androidx.lifecycle.LiveData
import java.util.Date

class OrderRepository(
    private val menuItemDao: MenuItemDao,
    private val orderDao: OrderDao
) {

    val allMenuItems: LiveData<List<MenuItem>> = menuItemDao.getAllMenuItems()

    /**
     * UPDATED: Now accepts a paymentMethod to be saved with the order.
     */
    suspend fun insertOrder(items: Map<MenuItem, Int>, paymentMethod: PaymentMethod) {
        if (items.isEmpty()) return

        val total = items.entries.sumOf { (menuItem, quantity) ->
            menuItem.price * quantity
        }

        // THE FIX: We now pass the paymentMethod when creating the Order object.
        val order = Order(
            timestamp = Date(),
            totalAmount = total,
            paymentMethod = paymentMethod
        )
        val orderId = orderDao.insertOrder(order)

        val orderItems = items.map { (menuItem, quantity) ->
            OrderItem(
                orderId = orderId,
                menuItemId = menuItem.id,
                quantity = quantity,
                price = menuItem.price
            )
        }

        orderDao.insertOrderItems(orderItems)
    }
}