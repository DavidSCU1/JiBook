package com.example.jibook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.jibook.models.Budget
import java.text.SimpleDateFormat
import java.util.*

class BudgetAdapter(
    private val onDeleteClick: (Budget) -> Unit
) : ListAdapter<Budget, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {
    
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget, parent, false)
        return BudgetViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_budget_name)
        private val amountText: TextView = itemView.findViewById(R.id.text_budget_amount)
        private val dateText: TextView = itemView.findViewById(R.id.text_budget_date)
        private val deleteButton: Button = itemView.findViewById(R.id.btn_delete_budget)
        
        fun bind(budget: Budget) {
            nameText.text = budget.name
            amountText.text = "¥${String.format("%.2f", budget.amount)}"
            dateText.text = "${dateFormat.format(Date(budget.startDate))} - ${dateFormat.format(Date(budget.endDate))}"
            
            deleteButton.setOnClickListener {
                onDeleteClick(budget)
            }
        }
    }
}

class BudgetDiffCallback : DiffUtil.ItemCallback<Budget>() {
    override fun areItemsTheSame(oldItem: Budget, newItem: Budget): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Budget, newItem: Budget): Boolean {
        return oldItem == newItem
    }
}