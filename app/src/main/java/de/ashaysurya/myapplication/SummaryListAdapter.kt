package de.ashaysurya.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class SummaryListAdapter :
    ListAdapter<DailySalesSummary, SummaryListAdapter.SummaryViewHolder>(SummaryComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.summary_list_item, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        private val totalTextView: TextView = itemView.findViewById(R.id.textViewDailyTotal)

        fun bind(summary: DailySalesSummary) {
            dateTextView.text = summary.saleDate
            totalTextView.text = String.format("₹%.2f", summary.total)
        }
    }

    class SummaryComparator : DiffUtil.ItemCallback<DailySalesSummary>() {
        override fun areItemsTheSame(oldItem: DailySalesSummary, newItem: DailySalesSummary): Boolean {
            return oldItem.saleDate == newItem.saleDate
        }

        override fun areContentsTheSame(oldItem: DailySalesSummary, newItem: DailySalesSummary): Boolean {
            return oldItem == newItem
        }
    }
}