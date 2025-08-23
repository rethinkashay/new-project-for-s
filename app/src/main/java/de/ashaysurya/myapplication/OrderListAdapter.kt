// File: OrderListAdapter.kt
package de.ashaysurya.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/**
 * UPDATED: The adapter now takes two callback functions for incrementing and decrementing.
 */
class OrderListAdapter(
    private val onIncrement: (MenuItem) -> Unit,
    private val onDecrement: (MenuItem) -> Unit
) : ListAdapter<Pair<MenuItem, Int>, OrderListAdapter.OrderViewHolder>(OrderComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.order_list_item, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val (menuItem, quantity) = getItem(position)
        // Pass the callbacks to the ViewHolder's bind function
        holder.bind(menuItem, quantity, onIncrement, onDecrement)
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Find all the new views from the layout
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewItemName)
        private val priceTextView: TextView = itemView.findViewById(R.id.textViewItemPrice)
        private val quantityTextView: TextView = itemView.findViewById(R.id.textViewQuantity)
        private val incrementButton: MaterialButton = itemView.findViewById(R.id.buttonIncrement)
        private val decrementButton: MaterialButton = itemView.findViewById(R.id.buttonDecrement)

        fun bind(
            item: MenuItem,
            quantity: Int,
            onIncrement: (MenuItem) -> Unit,
            onDecrement: (MenuItem) -> Unit
        ) {
            nameTextView.text = item.name
            // This now shows the price per item
            priceTextView.text = String.format("₹%.2f", item.price)
            quantityTextView.text = quantity.toString()

            // Set click listeners on the new buttons
            incrementButton.setOnClickListener { onIncrement(item) }
            decrementButton.setOnClickListener { onDecrement(item) }
        }
    }

    class OrderComparator : DiffUtil.ItemCallback<Pair<MenuItem, Int>>() {
        override fun areItemsTheSame(oldItem: Pair<MenuItem, Int>, newItem: Pair<MenuItem, Int>): Boolean {
            return oldItem.first.id == newItem.first.id
        }

        override fun areContentsTheSame(oldItem: Pair<MenuItem, Int>, newItem: Pair<MenuItem, Int>): Boolean {
            return oldItem == newItem
        }
    }
}