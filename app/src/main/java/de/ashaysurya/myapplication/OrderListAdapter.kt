package de.ashaysurya.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for the RecyclerView that displays the items in the current order.
 */
class OrderListAdapter :
    ListAdapter<Pair<MenuItem, Int>, OrderListAdapter.OrderViewHolder>(OrderComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.order_list_item, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val (menuItem, quantity) = getItem(position)
        holder.bind(menuItem, quantity)
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quantityTextView: TextView = itemView.findViewById(R.id.textViewQuantity)
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewItemName)
        private val totalTextView: TextView = itemView.findViewById(R.id.textViewItemTotal)

        fun bind(item: MenuItem, quantity: Int) {
            quantityTextView.text = "${quantity}x"
            nameTextView.text = item.name
            val itemTotal = item.price * quantity
            totalTextView.text = String.format("₹%.2f", itemTotal)
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