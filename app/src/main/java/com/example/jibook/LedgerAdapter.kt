package com.example.jibook.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jibook.R
import com.example.jibook.models.Ledger
import com.example.jibook.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LedgerAdapter(private val onClick: (Ledger) -> Unit, private val onDelete: (Ledger) -> Unit, private val context: Context) : RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder>() {

    private var ledgers: List<Ledger> = emptyList()

    fun submitList(newLedgers: List<Ledger>) {
        ledgers = newLedgers
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int) {
        if (position >= 0 && position < ledgers.size) {
            val ledgerToDelete = ledgers[position]
            onDelete(ledgerToDelete)
        }
        ledgers = ledgers.toMutableList().also { it.removeAt(position) }
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ledger, parent, false)
        return LedgerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LedgerViewHolder, position: Int) {
        val ledger = ledgers[position]
        holder.bind(ledger)
        holder.itemView.setOnClickListener { onClick(ledger) }
    }

    override fun getItemCount(): Int = ledgers.size

    override fun onViewRecycled(holder: LedgerViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    inner class LedgerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.text_ledger_item)
        private val recordCountTextView: TextView = itemView.findViewById(R.id.text_ledger_record_count)
        private var countJob: Job? = null
    
        fun bind(ledger: Ledger) {
            nameTextView.text = ledger.name
            
            // 取消之前的协程
            countJob?.cancel()
            
            // 动态获取记录数
            countJob = CoroutineScope(Dispatchers.IO).launch {
                val recordDao = AppDatabase.getDatabase(context).recordDao()
                recordDao.getRecordsForLedger(ledger.id).collect { records ->
                    withContext(Dispatchers.Main) {
                        recordCountTextView.text = "${records.size}条记录"
                    }
                }
            }
        }
        
        fun unbind() {
            countJob?.cancel()
        }
    }
}