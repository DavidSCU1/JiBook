package com.example.jibook.manager

import android.content.Context
import com.example.jibook.models.Budget  
import com.example.jibook.models.Record
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class AnalysisResult(
    val analysis: String,
    val chartData: String
)

class AIAnalysisManager(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val apiKey = "aa32e306-cd86-4f3f-8d48-56b7db695f84"
    private val baseUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
    
    // 简化分析结果，只返回综合分析
    suspend fun generateComprehensiveAnalysis(records: List<Record>, budgets: List<Budget>): AnalysisResult {
        return withContext(Dispatchers.IO) {
            try {
                val dataString = buildAnalysisDataString(records, budgets)
                val analysisPrompt = buildAnalysisPrompt(dataString)
                val chartPrompt = buildChartPrompt(dataString)
                
                val analysis = callDoubaoAPI(analysisPrompt)
                val chartData = callDoubaoAPI(chartPrompt)
                
                AnalysisResult(analysis, chartData)
            } catch (e: Exception) {
                AnalysisResult("分析生成失败：${e.message}", "图表生成失败：${e.message}")
            }
        }
    }
    
    private fun buildAnalysisDataString(records: List<Record>, budgets: List<Budget>): String {
        val totalIncome = records.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpense = records.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
        val netAmount = totalIncome - totalExpense
        
        // 按类型分组
        val groupedByType = records.groupBy { it.type }
        
        // 按月份统计
        val monthlyData = records.groupBy { 
            val date = Date(it.creationDate)
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(date)
        }.map { (month, recordList) ->
            val monthIncome = recordList.filter { it.amount > 0 }.sumOf { it.amount }
            val monthExpense = recordList.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
            month to Pair(monthIncome, monthExpense)
        }.sortedBy { it.first }
        
        // 预算分析
        val currentTime = System.currentTimeMillis()
        val activeBudgets = budgets.filter { it.startDate <= currentTime && it.endDate >= currentTime }
        val budgetAnalysis = activeBudgets.map { budget ->
            val budgetExpense = records.filter { record ->
                record.creationDate >= budget.startDate && 
                record.creationDate <= budget.endDate && 
                record.amount < 0
            }.sumOf { kotlin.math.abs(it.amount) }
            
            val usagePercentage = if (budget.amount > 0) (budgetExpense / budget.amount * 100) else 0.0
            val remaining = budget.amount - budgetExpense
            
            "${budget.name}: 预算¥${String.format("%.2f", budget.amount)}, 已用¥${String.format("%.2f", budgetExpense)} (${String.format("%.1f", usagePercentage)}%), 剩余¥${String.format("%.2f", remaining)}"
        }
        
        return buildString {
            appendLine("=== 财务综合分析 ===\n")
            appendLine("📊 总体概况:")
            appendLine("总收入: ¥${String.format("%.2f", totalIncome)}")
            appendLine("总支出: ¥${String.format("%.2f", totalExpense)}")
            appendLine("净收支: ¥${String.format("%.2f", netAmount)}")
            appendLine("记录数量: ${records.size}条\n")
            
            if (budgetAnalysis.isNotEmpty()) {
                appendLine("💰 预算执行情况:")
                budgetAnalysis.forEach { appendLine(it) }
                appendLine()
            }
            
            appendLine("📈 分类统计:")
            groupedByType.forEach { (type, recordList) ->
                val categoryTotal = recordList.sumOf { kotlin.math.abs(it.amount) }
                appendLine("$type: ¥${String.format("%.2f", categoryTotal)}")
            }
            appendLine()
            
            appendLine("📅 月度趋势:")
            monthlyData.forEach { (month, amounts) ->
                appendLine("$month: 收入¥${String.format("%.2f", amounts.first)}, 支出¥${String.format("%.2f", amounts.second)}")
            }
        }
    }
    
    private fun buildAnalysisPrompt(dataString: String): String {
        return """
            作为专业的财务分析师，请基于以下数据生成简洁的综合财务分析报告：
            
            $dataString
            
            请提供一个统一的综合分析，包含：
            📊 财务状况总结
            💡 关键发现和建议
            ⚠️ 风险提示
            🎯 改进建议
            
            要求：
            - 语言简洁专业
            - 重点突出
            - 实用性强
            - 使用emoji增强可读性
            - 不要分小标题，统一叙述
        """.trimIndent()
    }
    
    private fun buildChartPrompt(dataString: String): String {
        return """
            基于以下财务数据，生成JSON格式的图表数据：
            $dataString
            
            请返回JSON格式，包含以下结构：
            {
                "chartData": [
                    {"label": "类别名称", "value": 数值, "color": "#颜色代码"},
                    ...
                ]
            }
            
            要求：
            1. value为实际金额或百分比数值
            2. color使用十六进制颜色代码
            3. 只返回JSON，不要其他文字
        """.trimIndent()
    }
    
    private suspend fun callDoubaoAPI(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject().apply {
                    // 更新模型名称为官方示例中的
                    put("model", "doubao-1-5-lite-32k-250115")
                    put("messages", JSONArray().apply {
                        // 添加系统消息
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "你是人工智能助手.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("max_tokens", 2000)
                    put("temperature", 0.7)
                }
                
                val request = Request.Builder()
                    .url(baseUrl)
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val message = firstChoice.getJSONObject("message")
                        message.getString("content")
                    } else {
                        "API响应格式错误"
                    }
                } else {
                    val errorBody = response.body?.string() ?: ""
                    "API调用失败: ${response.code} - $errorBody"
                }
            } catch (e: IOException) {
                "网络请求失败: ${e.message}"
            } catch (e: Exception) {
                "处理失败: ${e.message}"
            }
        }
    }
}