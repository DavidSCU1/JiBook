package com.example.jibook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jibook.adapters.RecordAdapter
import com.example.jibook.data.AppDatabase
import com.example.jibook.manager.FinancialAdviceManager
import com.example.jibook.AddRecordActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LedgerDetailActivity : ComponentActivity() {
    private var ledgerId: Long = -1
    private lateinit var recordAdapter: RecordAdapter

    // 记录删除广播接收器
    private val recordDeletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == RecordAdapter.ACTION_RECORD_DELETED) {
                val deletedLedgerId = intent.getLongExtra("ledgerId", -1)
                if (deletedLedgerId == ledgerId) {
                    Log.d("LedgerDetailActivity", "接收到记录删除广播，开始刷新记录列表")
                    loadRecordsFromDatabase()
                }
            }
        }
    }

    // 记录添加广播接收器
    private val recordAddedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.jibook.action.RECORD_ADDED") {
                Log.d("LedgerDetailActivity", "接收到记录添加广播，开始刷新记录列表")
                loadRecordsFromDatabase()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ledger_detail)

        // 获取账本ID
        ledgerId = intent.getLongExtra("ledgerId", -1)
        if (ledgerId == -1L) {
            Toast.makeText(this, "获取账本ID失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 设置账本名称
        val ledgerName = intent.getStringExtra("ledgerName")
        if (ledgerName != null) {
            findViewById<TextView>(R.id.text_ledger_name).text = ledgerName
        } else {
            findViewById<TextView>(R.id.text_ledger_name).text = "未知账本"
        }

        // 初始化RecyclerView - 修复构造函数参数
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_records)
        recordAdapter = RecordAdapter(this)  // 只传递context参数
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recordAdapter

        // 设置滑动删除 - 使用正确的方法
        recordAdapter.setupSwipeToDelete(recyclerView)

        // 注册广播接收器
        registerReceivers()

        // 加载记录数据
        loadRecordsFromDatabase()

        // 添加记录按钮
        findViewById<Button>(R.id.btn_add_record).setOnClickListener {
            val intent = Intent(this, AddRecordActivity::class.java)
            intent.putExtra("ledgerId", ledgerId)
            startActivityForResult(intent, 1)
        }

        // 在onCreate方法中，删除或注释掉以下代码：
        // 查看图表按钮
        // findViewById<Button>(R.id.btn_view_chart).setOnClickListener {
        //     val intent = Intent(this, PieChartActivity::class.java)
        //     intent.putExtra("ledgerId", ledgerId)
        //     startActivity(intent)
        // }

        // 删除理财建议按钮的点击事件处理
        // findViewById<Button>(R.id.btn_financial_advice).setOnClickListener {
        //     val intent = Intent(this, FinancialAdviceActivity::class.java)
        //     intent.putExtra("ledgerId", ledgerId)
        //     startActivity(intent)
        // }
    }

    private fun registerReceivers() {
        // 注册记录删除广播接收器
        val deleteFilter = IntentFilter(RecordAdapter.ACTION_RECORD_DELETED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(recordDeletedReceiver, deleteFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(recordDeletedReceiver, deleteFilter)
        }

        // 注册记录添加广播接收器
        val addFilter = IntentFilter("com.example.jibook.action.RECORD_ADDED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(recordAddedReceiver, addFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(recordAddedReceiver, addFilter)
        }
    }

    private fun loadRecordsFromDatabase() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@LedgerDetailActivity)
                if (db != null) {
                    // 修复：使用records属性而不是submitList方法
                    db.recordDao().getRecordsForLedger(ledgerId).collect { records ->
                        recordAdapter.records = records
                    }
                } else {
                    Log.e("LedgerDetailActivity", "数据库获取失败")
                    recordAdapter.records = emptyList()
                }
            } catch (e: Exception) {
                Log.e("LedgerDetailActivity", "加载记录失败: ${e.message}")
                recordAdapter.records = emptyList()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadRecordsFromDatabase()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(recordDeletedReceiver)
            unregisterReceiver(recordAddedReceiver)
        } catch (e: Exception) {
            Log.e("LedgerDetailActivity", "注销广播接收器失败: ${e.message}")
        }
    }
}