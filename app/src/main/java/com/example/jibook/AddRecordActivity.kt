package com.example.jibook

import android.widget.RadioGroup
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.jibook.data.AppDatabase
import com.example.jibook.models.Record
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AlertDialog

class AddRecordActivity : ComponentActivity() {

    private var ledgerId: Long = -1
    private lateinit var typeSpinner: Spinner
    private var isIncome = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_record)

        ledgerId = intent.getLongExtra("ledgerId", -1)
        if (ledgerId == -1L) {
            Toast.makeText(this, "获取账本ID失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化视图
        val radioGroup = findViewById<RadioGroup>(R.id.radio_group_inout)
        typeSpinner = findViewById<Spinner>(R.id.spinner_type)

        // 在RadioGroup的监听器中明确指定参数类型
        radioGroup.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            isIncome = checkedId == R.id.radio_income
            val typeList = getTypeListByMode(isIncome)
            val adapter = ArrayAdapter(this@AddRecordActivity, android.R.layout.simple_spinner_item, typeList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            typeSpinner.adapter = adapter
        }
        
        // 初始化
        val types = getTypeListByMode(false)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter

        findViewById<Button>(R.id.btn_save_record).setOnClickListener {
            // 获取输入内容
            val name = findViewById<EditText>(R.id.edit_record_name).text.toString().trim()
            val amountStr = findViewById<EditText>(R.id.edit_record_amount).text.toString().trim()
            val type = typeSpinner.selectedItem.toString()
            val note = findViewById<EditText>(R.id.edit_record_note).text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "请填写名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "请填写金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "金额格式不正确", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 修复：支出存储为负数，收入存储为正数
            val finalAmount = if (isIncome) amount else -amount
            
            // 创建记录
            val record = Record(
                ledgerId = ledgerId,
                name = name,
                amount = finalAmount,  // 使用修正后的金额
                type = type,
                note = note,
                creationDate = System.currentTimeMillis()
            )

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@AddRecordActivity).recordDao().insert(record)
                }
                // 发送新增记录广播
                val intent = Intent("com.example.jibook.action.RECORD_ADDED")
                sendBroadcast(intent)
                // 返回结果
                setResult(RESULT_OK)
                finish()
            }
        }
        
        findViewById<Button>(R.id.btn_manage_types).setOnClickListener {
            val types = getTypeListByMode(isIncome)
            val input = EditText(this)
            input.hint = "输入新类型"
            AlertDialog.Builder(this)
                .setTitle("管理类型")
                .setItems(types.toTypedArray()) { _, which ->
                    // 点击类型弹出删除确认
                    AlertDialog.Builder(this)
                        .setTitle("删除类型")
                        .setMessage("确定要删除类型：${types[which]} 吗？")
                        .setPositiveButton("删除") { _, _ ->
                            types.removeAt(which)
                            saveTypeListByMode(types.toSet(), isIncome)
                            updateSpinnerAdapter()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                .setView(input)
                .setPositiveButton("添加") { _, _ ->
                    val newType = input.text.toString().trim()
                    if (newType.isNotEmpty() && !types.contains(newType)) {
                        types.add(newType)
                        saveTypeListByMode(types.toSet(), isIncome)
                        updateSpinnerAdapter()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    // 确保返回键正常工作
    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun getTypeListByMode(isIncome: Boolean): MutableList<String> {
        val prefs = getSharedPreferences("record_types", MODE_PRIVATE)
        return if (isIncome) {
            prefs.getStringSet("income_types", setOf("工资", "理财", "其他"))!!.toMutableList()
        } else {
            prefs.getStringSet("expense_types", setOf("餐饮", "购物", "交通", "娱乐"))!!.toMutableList()
        }
    }

    private fun saveTypeListByMode(types: Set<String>, isIncome: Boolean) {
        val prefs = getSharedPreferences("record_types", MODE_PRIVATE)
        prefs.edit().putStringSet(if (isIncome) "income_types" else "expense_types", types).apply()
    }

    private fun updateSpinnerAdapter() {
        val typeList = getTypeListByMode(isIncome)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter
    }

    private fun getTypeList(): MutableList<String> {
        val prefs = getSharedPreferences("record_types", MODE_PRIVATE)
        return prefs.getStringSet("types", setOf("餐饮", "购物", "交通", "娱乐"))!!.toMutableList()
    }

    private fun saveTypeList(types: Set<String>) {
        val prefs = getSharedPreferences("record_types", MODE_PRIVATE)
        prefs.edit().putStringSet("types", types).apply()
    }
    
    private fun showAddCustomTypeDialog(isIncome: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(if (isIncome) "添加收入类型" else "添加支出类型")
        
        val input = EditText(this)
        input.hint = "请输入新的类型名称"
        builder.setView(input)
        
        builder.setPositiveButton("确定") { _, _ ->
            val newType = input.text.toString().trim()
            if (newType.isNotEmpty()) {
                addCustomType(newType, isIncome)
            }
        }
        
        builder.setNegativeButton("取消", null)
        builder.show()
    }
    
    private fun addCustomType(typeName: String, isIncome: Boolean) {
        val prefs = getSharedPreferences("record_types", MODE_PRIVATE)
        val key = if (isIncome) "income_types" else "expense_types"
        val currentTypes = prefs.getStringSet(key, setOf())!!.toMutableSet()
        
        if (!currentTypes.contains(typeName)) {
            currentTypes.add(typeName)
            prefs.edit().putStringSet(key, currentTypes).apply()
            
            // 刷新Spinner
            updateSpinnerAdapter()
            
            // 选中新添加的类型
            val typeList = getTypeListByMode(isIncome)
            val newIndex = typeList.indexOf(typeName)
            if (newIndex >= 0) {
                typeSpinner.setSelection(newIndex)
            }
            
            Toast.makeText(this, "已添加新类型：$typeName", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "该类型已存在", Toast.LENGTH_SHORT).show()
        }
    }
}