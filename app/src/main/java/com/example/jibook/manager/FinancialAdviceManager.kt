package com.example.jibook.manager

import android.content.Context
import android.util.Log
import com.example.jibook.models.Record
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

class FinancialAdviceManager(private val context: Context) {
    companion object {
        private const val TAG = "FinancialAdviceManager"
        // 更新为您的实际API密钥
        private const val API_KEY = "Replace with your own api or the agent will keep sleeping"
        // 更新API端点URL
        private const val API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
        // 更新模型名称
        private const val MODEL_NAME = "doubao-1-5-lite-32k-250115"
    }
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getSpendingAnalysis(records: List<Record>): String {
        return withContext(Dispatchers.IO) {
            try {
                val spendingData = buildSpendingDataString(records)
                val prompt = buildDoubaoPrompt(spendingData)
                val response = callDoubaoAPI(prompt)
                response
            } catch (e: Exception) {
                Log.e(TAG, "获取建议失败: ${e.message}")
                "获取理财建议失败，请检查网络连接"
            }
        }
    }

    private fun buildSpendingDataString(records: List<Record>): String {
        if (records.isEmpty()) return "没有支出记录"

        // 分离收入和支出记录
        val incomeRecords = records.filter { it.amount > 0 }
        val expenseRecords = records.filter { it.amount < 0 }
        
        if (expenseRecords.isEmpty()) return "没有支出记录"

        // 按类别统计支出（取绝对值）
        val categorySpending = expenseRecords.groupBy { it.type }
            .mapValues { it.value.sumOf { r -> kotlin.math.abs(r.amount) } }

        // 计算总支出（取绝对值）
        val totalSpending = expenseRecords.sumOf { kotlin.math.abs(it.amount) }
        
        // 计算总收入
        val totalIncome = incomeRecords.sumOf { it.amount }

        // 找出最大和最小支出类别
        val maxCategory = categorySpending.maxByOrNull { it.value }?.key ?: ""
        val minCategory = categorySpending.minByOrNull { it.value }?.key ?: ""

        // 构建数据字符串
        var dataString = "最近财务数据:\n"
        dataString += "总收入: ¥${"%.2f".format(totalIncome)}\n"
        dataString += "总支出: ¥${"%.2f".format(totalSpending)}\n"
        dataString += "结余: ¥${"%.2f".format(totalIncome - totalSpending)}\n\n"
        dataString += "分类支出:\n"

        categorySpending.forEach { (category, amount) ->
            val percentage = "%.2f".format((amount / totalSpending) * 100)
            dataString += "- $category: ¥${"%.2f".format(amount)} (${percentage}%)\n"
        }

        dataString += "\n支出分析:\n"
        dataString += "- 最大支出类别: $maxCategory\n"
        dataString += "- 最小支出类别: $minCategory\n"

        return dataString
    }

    private fun buildDoubaoPrompt(spendingData: String): String {
        return """
            我是一个个人财务管理助手，请根据以下支出数据提供财务建议:
            
            $spendingData
            
            请提供以下建议:
            1. 支出模式分析（例如：哪些类别支出过高/过低）
            2. 节省建议（例如：可以削减哪些不必要的开支）
            3. 理财优化建议（例如：如何分配资金更合理）
            4. 个性化的预算规划（按类别给出建议比例）
            5. 1-2条适合用户的理财小知识
            
            请以友好、简洁的方式回答，避免过于技术性的语言，控制在300字以内。
        """.trimIndent()
    }

    private suspend fun callDoubaoAPI(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject().apply {
                    put("model", MODEL_NAME)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "你是一个专业的财务顾问助手")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }

                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "API Response Code: ${response.code}")
                    Log.d(TAG, "API Response Message: ${response.message}")
                    
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        Log.e(TAG, "API Error: $errorBody")
                        return@use when (response.code) {
                            401 -> "API密钥无效，请检查配置"
                            403 -> "API访问被拒绝，请检查权限"
                            404 -> "API端点不存在，请检查URL配置"
                            429 -> "API调用频率过高，请稍后重试"
                            else -> "连接服务失败，错误代码：${response.code}"
                        }
                    }

                    val responseBody = response.body?.string()
                    Log.d(TAG, "API Response Body: $responseBody")
                    
                    val responseData = gson.fromJson(
                        responseBody,
                        DoubaoResponse::class.java
                    )

                    responseData.choices.firstOrNull()?.message?.content ?: "无法获取建议"
                }
            } catch (e: Exception) {
                Log.e(TAG, "API调用失败", e)
                "连接服务失败，请检查网络连接和API配置"
            }
        }
    }

    // 用于解析豆包API响应的类
    data class DoubaoResponse(
        val choices: List<Choice>
    )

    data class Choice(
        val message: Message
    )

    data class Message(
        val content: String
    )

    // 获取随机理财小贴士
    suspend fun getFinancialTip(): String {
        // 实际应用中可以定期从服务器获取新的小贴士
        // 这里为简化示例，使用预设的一些小贴士
        val tips = listOf(
            "每月将收入的20%存入紧急备用金账户，确保6个月的生活费用储备",
            "使用50/30/20预算法：50%用于必需品，30%用于非必需品，20%用于储蓄和投资",
            "设置自动转账，每月工资到账后优先完成储蓄计划",
            "定期审查订阅服务，取消不再使用的会员资格",
            "利用复利效应，尽早开始投资，即使是小额资金",
            "为不同的财务目标创建单独的储蓄账户",
            "建立预算时，为意外支出预留空间",
            "比较不同银行的储蓄利率，选择收益更高的产品",
            "使用现金返还信用卡支付日常开销，但要每月全额还款",
            "定期评估保险需求，确保保障充足但不过度"
        )

        // 模拟API调用延迟
        kotlinx.coroutines.delay(1000)

        return tips.random()
    }
}
