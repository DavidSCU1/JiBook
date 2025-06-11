package com.example.jibook.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.jibook.R
import com.example.jibook.data.AppDatabase
import com.example.jibook.models.Record
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter(private val context: Context) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    companion object {
        const val ACTION_RECORD_DELETED = "com.example.jibook.RECORD_DELETED"
    }

    var records: List<Record> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.text_record_name)
        private val typeTextView: TextView = itemView.findViewById(R.id.text_record_type)
        private val amountTextView: TextView = itemView.findViewById(R.id.text_record_amount)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_record_date)
    
        fun bind(record: Record) {
            nameTextView.text = record.name
            typeTextView.text = record.type
            amountTextView.text = String.format("%.2f", record.amount)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            dateTextView.text = dateFormat.format(Date(record.creationDate))
        }
    }

    // 滑动删除功能
    fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback())
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    inner class SwipeToDeleteCallback : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val deletedRecord = records[position]
            
            // 从数据库删除记录
            CoroutineScope(Dispatchers.IO).launch {
                val recordDao = AppDatabase.getDatabase(context).recordDao()
                recordDao.delete(deletedRecord.id)
            }
            
            // 从列表中移除
            records = records.toMutableList().also { it.removeAt(position) }
            notifyItemRemoved(position)

            // 发送删除广播
            val intent = Intent(ACTION_RECORD_DELETED)
            intent.putExtra("ledgerId", deletedRecord.ledgerId)
            context.sendBroadcast(intent)
        }
    }
}