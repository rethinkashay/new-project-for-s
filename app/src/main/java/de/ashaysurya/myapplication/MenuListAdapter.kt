package de.ashaysurya.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

// Add a parameter to the constructor for the click listener
class MenuListAdapter(private val onItemClicked: (MenuItem) -> Unit) : ListAdapter<MenuItem, MenuListAdapter.MenuItemViewHolder>(MenuItemsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        return MenuItemViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        val current = getItem(position)
        // Set the click listener for the item view
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }
        holder.bind(current.name, current.price)
    }

    class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameItemView: TextView = itemView.findViewById(R.id.textViewName)
        private val priceItemView: TextView = itemView.findViewById(R.id.textViewPrice)

        fun bind(name: String?, price: Double?) {
            nameItemView.text = name
            priceItemView.text = String.format("₹%.2f", price)
        }

        companion object {
            fun create(parent: ViewGroup): MenuItemViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item, parent, false)
                return MenuItemViewHolder(view)
            }
        }
    }

    class MenuItemsComparator : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem == newItem
        }
    }
}