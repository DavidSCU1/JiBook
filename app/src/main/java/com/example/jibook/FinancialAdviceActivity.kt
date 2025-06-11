package com.example.jibook

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.jibook.data.AppDatabase
import com.example.jibook.manager.FinancialAdviceManager
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class FinancialAdviceActivity : ComponentActivity() {

    private var ledgerId: Long = -1
    private lateinit var adviceTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adviceManager: FinancialAdviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_advice)

        ledgerId = intent.getLongExtra("ledgerId", -1)
        if (ledgerId == -1L) {
            Toast.makeText(this, "获取账本ID失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adviceTextView = findViewById(R.id.text_advice)
        progressBar = findViewById(R.id.progress_bar)
        adviceManager = FinancialAdviceManager(this)

        // 返回按钮
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // 重新加载建议按钮
        findViewById<Button>(R.id.btn_refresh).setOnClickListener {
            getFinancialAdvice()
        }

        // 获取并显示理财建议
        getFinancialAdvice()
    }

    private fun getFinancialAdvice() {
        progressBar.visibility = View.VISIBLE
        adviceTextView.text = ""

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@FinancialAdviceActivity)
                if (db != null) {
                    // 修复：正确处理Flow类型
                    db.recordDao().getRecordsForLedger(ledgerId).collect { records ->
                        if (records.isEmpty()) {
                            progressBar.visibility = View.GONE
                            adviceTextView.text = "没有足够的支出数据来生成建议"
                            return@collect
                        }

                        // 调用豆包API获取分析建议
                        val advice = withContext(Dispatchers.IO) {
                            adviceManager.getSpendingAnalysis(records)
                        }

                        progressBar.visibility = View.GONE
                        adviceTextView.text = advice
                    }
                } else {
                    progressBar.visibility = View.GONE
                    adviceTextView.text = "数据库初始化失败"
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                adviceTextView.text = "获取建议失败: ${e.message}"
            }
        }
    }
}