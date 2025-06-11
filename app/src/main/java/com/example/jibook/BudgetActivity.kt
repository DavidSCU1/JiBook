package com.example.jibook

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jibook.data.AppDatabase
import com.example.jibook.models.Budget
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BudgetActivity : AppCompatActivity() {
    
    private lateinit var ledgerSpinner: Spinner
    private lateinit var budgetNameEdit: EditText
    private lateinit var budgetAmountEdit: EditText
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var addBudgetButton: Button
    private lateinit var budgetRecyclerView: RecyclerView
    
    private lateinit var budgetDao: com.example.jibook.data.BudgetDao
    private lateinit var ledgerDao: com.example.jibook.data.LedgerDao
    private lateinit var budgetAdapter: BudgetAdapter
    
    private var selectedLedgerId: Long = -1
    private var startDate: Long = 0
    private var endDate: Long = 0
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)
        
        budgetDao = AppDatabase.getDatabase(this).budgetDao()
        ledgerDao = AppDatabase.getDatabase(this).ledgerDao()
        
        initViews()
        setupLedgerSpinner()
        setupListeners()
        setupRecyclerView()
    }
    
    private fun initViews() {
        ledgerSpinner = findViewById(R.id.spinner_budget_ledger)
        budgetNameEdit = findViewById(R.id.edit_budget_name)
        budgetAmountEdit = findViewById(R.id.edit_budget_amount)
        startDateButton = findViewById(R.id.btn_start_date)
        endDateButton = findViewById(R.id.btn_end_date)
        addBudgetButton = findViewById(R.id.btn_add_budget)
        budgetRecyclerView = findViewById(R.id.recycler_budgets)
    }
    
    private fun setupLedgerSpinner() {
        lifecycleScope.launch {
            ledgerDao.getAllLedgers().collect { ledgers ->
                val ledgerNames = ledgers.map { it.name }
                val adapter = ArrayAdapter(this@BudgetActivity, android.R.layout.simple_spinner_item, ledgerNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                ledgerSpinner.adapter = adapter
                
                ledgerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                        if (ledgers.isNotEmpty()) {
                            selectedLedgerId = ledgers[position].id
                            loadBudgets()
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                
                if (ledgers.isNotEmpty()) {
                    selectedLedgerId = ledgers[0].id
                    loadBudgets()
                }
            }
        }
    }
    
    private fun setupListeners() {
        startDateButton.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                startDateButton.text = dateFormat.format(Date(date))
            }
        }
        
        endDateButton.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                endDateButton.text = dateFormat.format(Date(date))
            }
        }
        
        addBudgetButton.setOnClickListener {
            addBudget()
        }
    }
    
    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter { budget ->
            deleteBudget(budget)
        }
        budgetRecyclerView.layoutManager = LinearLayoutManager(this)
        budgetRecyclerView.adapter = budgetAdapter
    }
    
    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun addBudget() {
        val name = budgetNameEdit.text.toString().trim()
        val amountText = budgetAmountEdit.text.toString().trim()
        
        if (name.isEmpty() || amountText.isEmpty() || startDate == 0L || endDate == 0L || selectedLedgerId == -1L) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (startDate >= endDate) {
            Toast.makeText(this, "结束日期必须晚于开始日期", Toast.LENGTH_SHORT).show()
            return
        }
        
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "请输入有效的预算金额", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val budget = Budget(
                ledgerId = selectedLedgerId,
                name = name,
                amount = amount,
                startDate = startDate,
                endDate = endDate
            )
            budgetDao.insert(budget)
            
            // 清空输入
            budgetNameEdit.text.clear()
            budgetAmountEdit.text.clear()
            startDateButton.text = "选择开始日期"
            endDateButton.text = "选择结束日期"
            startDate = 0
            endDate = 0
            
            Toast.makeText(this@BudgetActivity, "预算添加成功", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadBudgets() {
        if (selectedLedgerId != -1L) {
            lifecycleScope.launch {
                budgetDao.getBudgetsByLedger(selectedLedgerId).collect { budgets ->
                    budgetAdapter.submitList(budgets)
                }
            }
        }
    }
    
    private fun deleteBudget(budget: Budget) {
        lifecycleScope.launch {
            budgetDao.delete(budget)
            Toast.makeText(this@BudgetActivity, "预算已删除", Toast.LENGTH_SHORT).show()
        }
    }
}