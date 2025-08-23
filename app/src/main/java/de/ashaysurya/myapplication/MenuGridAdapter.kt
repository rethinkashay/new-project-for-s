// File: MenuGridAdapter.kt
package de.ashaysurya.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

// Define constants for our two view types
private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

class MenuGridAdapter(private val onItemClicked: (MenuItem) -> Unit) :
    ListAdapter<DataItem, RecyclerView.ViewHolder>(DataItemDiffCallback()) {

    // This function is called by the adapter to determine which layout to use for a given position.
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.HeaderWrapper -> ITEM_VIEW_TYPE_HEADER
            is DataItem.MenuItemWrapper -> ITEM_VIEW_TYPE_ITEM
        }
    }

    // This function creates the correct ViewHolder based on the view type.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> CategoryHeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> MenuViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    // This function binds the data to the correct ViewHolder.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MenuViewHolder -> {
                val menuItemWrapper = getItem(position) as DataItem.MenuItemWrapper
                holder.bind(menuItemWrapper.menuItem, onItemClicked)
            }
            is CategoryHeaderViewHolder -> {
                val headerWrapper = getItem(position) as DataItem.HeaderWrapper
                holder.bind(headerWrapper.categoryName)
            }
        }
    }

    // ViewHolder for the menu item card
    class MenuViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewName)
        private val priceTextView: TextView = itemView.findViewById(R.id.textViewPrice)

        fun bind(item: MenuItem, onItemClicked: (MenuItem) -> Unit) {
            nameTextView.text = item.name
            priceTextView.text = String.format("₹%.2f", item.price)
            itemView.setOnClickListener {
                onItemClicked(item)
            }
        }

        companion object {
            fun from(parent: ViewGroup): MenuViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.grid_item_menu, parent, false)
                return MenuViewHolder(view)
            }
        }
    }

    // ViewHolder for the category header text
    class CategoryHeaderViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerTextView: TextView = itemView.findViewById(R.id.textViewCategoryHeader)

        fun bind(categoryName: String) {
            headerTextView.text = categoryName
        }

        companion object {
            fun from(parent: ViewGroup): CategoryHeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.list_item_category_header, parent, false)
                return CategoryHeaderViewHolder(view)
            }
        }
    }
}

// DiffUtil for our new DataItem sealed class.
class DataItemDiffCallback : DiffUtil.ItemCallback<DataItem>() {
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        // Headers are the same if their names are the same.
        // Menu items are the same if their IDs are the same.
        return (oldItem is DataItem.HeaderWrapper && newItem is DataItem.HeaderWrapper && oldItem.categoryName == newItem.categoryName) ||
                (oldItem is DataItem.MenuItemWrapper && newItem is DataItem.MenuItemWrapper && oldItem.menuItem.id == newItem.menuItem.id)
    }

    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }
}