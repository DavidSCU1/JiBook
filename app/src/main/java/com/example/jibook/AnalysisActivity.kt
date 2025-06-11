package com.example.jibook

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.jibook.data.AppDatabase
import com.example.jibook.data.LedgerDao
import com.example.jibook.data.RecordDao
import com.example.jibook.data.BudgetDao
import com.example.jibook.manager.AIAnalysisManager
import com.example.jibook.BudgetActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.json.JSONObject


class AnalysisActivity : AppCompatActivity() {
    
    private lateinit var ledgerSpinner: Spinner
    private lateinit var titleTextView: TextView
    private lateinit var btnStartAnalysis: Button
    private lateinit var btnManageBudget: Button
    private lateinit var analysisResultContainer: LinearLayout
    private lateinit var chartDataContainer: LinearLayout
    private lateinit var loadingContainer: LinearLayout
    private lateinit var textAnalysisResult: TextView
    private lateinit var pieChart: PieChart
    
    // 数据库DAO
    private lateinit var recordDao: com.example.jibook.data.RecordDao
    private lateinit var ledgerDao: com.example.jibook.data.LedgerDao
    private lateinit var budgetDao: com.example.jibook.data.BudgetDao
    private lateinit var aiAnalysisManager: AIAnalysisManager
    
    private var selectedLedgerId: Long = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)
        
        // 初始化数据库
        val database = AppDatabase.getDatabase(this)
        recordDao = database.recordDao()
        ledgerDao = database.ledgerDao()
        budgetDao = database.budgetDao()
        
        initViews()
        setupLedgerSpinner()
        setupListeners()
    }
    
    private fun initViews() {
        ledgerSpinner = findViewById(R.id.spinner_ledger_selection)
        titleTextView = findViewById(R.id.title_text_view)
        btnStartAnalysis = findViewById(R.id.btn_start_analysis)
        btnManageBudget = findViewById(R.id.btn_manage_budget)
        analysisResultContainer = findViewById(R.id.analysis_result_container)
        chartDataContainer = findViewById(R.id.chart_data_container)
        loadingContainer = findViewById(R.id.loading_container)
        textAnalysisResult = findViewById(R.id.text_analysis_result)
        
        aiAnalysisManager = AIAnalysisManager(this)
        pieChart = findViewById(R.id.pie_chart)
        setupPieChart()
    }
    
    private fun setupLedgerSpinner() {
        lifecycleScope.launch {
            ledgerDao.getAllLedgers().collect { ledgers ->
                val ledgerNames = ledgers.map { it.name }
                val adapter = ArrayAdapter(this@AnalysisActivity, android.R.layout.simple_spinner_item, ledgerNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                ledgerSpinner.adapter = adapter
                
                ledgerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (ledgers.isNotEmpty()) {
                            selectedLedgerId = ledgers[position].id
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                
                if (ledgers.isNotEmpty()) {
                    selectedLedgerId = ledgers[0].id
                }
            }
        }
    }
    
    private fun setupListeners() {
        btnStartAnalysis.setOnClickListener {
            if (selectedLedgerId != -1L) {
                startAIAnalysis()
            } else {
                Toast.makeText(this, "请先选择账本", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnManageBudget.setOnClickListener {
            val intent = Intent(this, BudgetActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun startAIAnalysis() {
        lifecycleScope.launch {
            try {
                showLoading(show = true)  // 修复: 添加参数
                
                val records = recordDao.getRecordsForLedger(selectedLedgerId).first()  // 修复: 方法名
                
                val budgets = budgetDao.getBudgetsByLedger(selectedLedgerId).first()
                
                if (records.isEmpty()) {
                    showLoading(false)  // 修复: 添加参数
                    hideResults()
                    Toast.makeText(this@AnalysisActivity, "该账本暂无记录", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                pieChart.centerText = "正在生成分析..."
                
                val result = aiAnalysisManager.generateComprehensiveAnalysis(records, budgets)
                
                textAnalysisResult.text = result.analysis
                
                if (result.chartData.isNotEmpty() && !result.chartData.contains("失败")) {
                    displayChartData(result.chartData)
                } else {
                    pieChart.centerText = "图表生成失败"
                    pieChart.visibility = View.GONE
                }
                
                showLoading(false)  // 修复: 添加参数
                showResults()
                
            } catch (e: Exception) {
                showLoading(show = false)  // 修复: 添加参数
                hideResults()
                Toast.makeText(this@AnalysisActivity, "该账本暂无记录", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(false) // 不使用百分比显示
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            setDrawHoleEnabled(true)
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            
            // 关键配置：隐藏所有文字和数值
            setDrawEntryLabels(false) // 不显示扇形区域的标签
            legend.isEnabled = false // 不显示图例
            
            // 设置点击监听器
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e is PieEntry) {
                        // 显示选中扇形的详细信息
                        val label = e.label
                        val value = e.value
                        val percentage = (value / data.yValueSum * 100)
                        
                        // 在中心显示详细信息
                        centerText = "$label\n${String.format("%.2f", value)}元\n${String.format("%.1f", percentage)}%"
                    }
                }
                
                override fun onNothingSelected() {
                    // 取消选择时清空中心文字
                    centerText = ""
                }
            })
        }
    }
    
    private fun displayChartData(chartDataString: String) {
        try {
            // 改进JSON解析逻辑
            val jsonObject = JSONObject(chartDataString)
            val chartDataArray = jsonObject.getJSONArray("chartData")
            
            val entries = mutableListOf<PieEntry>()
            val colors = mutableListOf<Int>()
            
            for (i in 0 until chartDataArray.length()) {
                val item = chartDataArray.getJSONObject(i)
                val label = item.getString("label")
                val value = item.getDouble("value").toFloat()
                val colorString = item.optString("color", "#FF6B6B")
                
                entries.add(PieEntry(value, label))
                
                // 解析颜色
                try {
                    val color = Color.parseColor(colorString)
                    colors.add(color)
                } catch (e: Exception) {
                    colors.add(Color.parseColor("#FF6B6B")) // 默认颜色
                }
            }
            
            if (entries.isNotEmpty()) {
                val dataSet = PieDataSet(entries, "财务分析")
                dataSet.colors = colors
                
                // 关键配置：隐藏数值显示
                dataSet.setDrawValues(false) // 不在扇形上显示数值
                dataSet.valueTextSize = 0f // 文字大小设为0
                
                // 设置选中时的效果
                dataSet.selectionShift = 10f // 选中时向外突出的距离
                
                val data = PieData(dataSet)
                pieChart.data = data
                
                // 清除中心文本
                pieChart.centerText = "点击查看详情"
                
                pieChart.invalidate()
                pieChart.visibility = View.VISIBLE
            } else {
                pieChart.centerText = "暂无数据"
                pieChart.visibility = View.GONE
                Toast.makeText(this, "No chart data available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            pieChart.centerText = "数据解析失败"
            pieChart.visibility = View.GONE
            Toast.makeText(this, "图表数据解析失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showLoading(show: Boolean) {
        loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showResults() {
        analysisResultContainer.visibility = View.VISIBLE
        chartDataContainer.visibility = View.VISIBLE
    }

    private fun hideResults() {
        analysisResultContainer.visibility = View.GONE
        chartDataContainer.visibility = View.GONE
    }
}