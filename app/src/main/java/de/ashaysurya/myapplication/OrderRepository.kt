package de.ashaysurya.myapplication

import androidx.lifecycle.LiveData
import java.util.Date

/**
 * Repository for handling all data operations related to Orders.
 */
class OrderRepository(
    private val menuItemDao: MenuItemDao,
    private val orderDao: OrderDao
) {

    // Get all menu items to display on the POS screen.
    val allMenuItems: LiveData<List<MenuItem>> = menuItemDao.getAllMenuItems()

    /**
     * Inserts a complete order into the database. This includes the main order record
     * and all of its associated line items.
     *
     * @param items A map where the key is the MenuItem and the value is the quantity.
     */
    suspend fun insertOrder(items: Map<MenuItem, Int>) {
        if (items.isEmpty()) return

        // Calculate the total amount for the order
        val total = items.entries.sumOf { (menuItem, quantity) ->
            menuItem.price * quantity
        }

        // Create the main order object
        val order = Order(timestamp = Date(), totalAmount = total)
        // Insert the order and get its newly generated ID
        val orderId = orderDao.insertOrder(order)

        // Create a list of OrderItem objects
        val orderItems = items.map { (menuItem, quantity) ->
            OrderItem(
                orderId = orderId,
                menuItemId = menuItem.id,
                quantity = quantity,
                price = menuItem.price // Save the price at the time of sale
            )
        }

        // Insert all the line items into the database
        orderDao.insertOrderItems(orderItems)
    }
}