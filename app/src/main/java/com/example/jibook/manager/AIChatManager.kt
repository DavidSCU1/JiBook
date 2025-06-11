package com.example.jibook.manager

import android.content.Context
import com.example.jibook.data.AppDatabase
import com.example.jibook.models.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class AIChatManager(private val context: Context) {
    companion object {
        private const val API_KEY = "aa32e306-cd86-4f3f-8d48-56b7db695f84"
        private const val API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
        private const val MODEL_NAME = "doubao-1-5-lite-32k-250115"
    }
    
    private val database = AppDatabase.getDatabase(context)
    private val ledgerDao = database.ledgerDao()
    private val recordDao = database.recordDao()
    private val userDao = database.userDao()
    private val chatMessageDao = database.chatMessageDao()
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var pendingLedgerCreation: PendingLedgerCreation? = null
    
    suspend fun processUserMessage(message: String, userId: Long): String {
        val response = when {
            pendingLedgerCreation != null && (message.contains("是") || message.contains("确认") || message.contains("创建")) -> {
                handleLedgerCreation(pendingLedgerCreation!!, userId)
            }
            pendingLedgerCreation != null && (message.contains("否") || message.contains("取消") || message.contains("不")) -> {
                pendingLedgerCreation = null
                "好的，已取消创建账本 😊 还有什么需要帮助的吗？"
            }
            else -> {
                processIntelligentMessage(message, userId)
            }
        }
        
        return response
    }
    
    private suspend fun processIntelligentMessage(message: String, userId: Long): String {
        return withContext(Dispatchers.IO) {
            try {
                val ledgers = ledgerDao.getLedgersByUserId(userId).first()
                val ledgerNames = ledgers.map { it.name }
                
                val prompt = buildIntelligentPrompt(message, ledgerNames)
                val aiResponse = callDoubaoAPI(prompt)
                
                parseIntelligentResponse(aiResponse, userId)
            } catch (e: Exception) {
                "抱歉，我遇到了一些问题 😅 请稍后再试，或者换个说法告诉我你的需求。"
            }
        }
    }
    
    private fun buildIntelligentPrompt(message: String, ledgerNames: List<String>): String {
        return """
            你是一个智能记账助手，具有以下能力：
            
            1. 记账功能：识别并记录收支信息
            2. 账本管理：创建、删除、查看账本
            3. 自然对话：友好、贴心的交流
            
            用户消息："$message"
            现有账本：${ledgerNames.joinToString(", ")}
            
            请分析用户意图并返回JSON：
            
            **记账意图**：
            {
                "type": "record",
                "amount": 数字,
                "ledgerName": "账本名",
                "description": "描述",
                "category": "类型",
                "isIncome": true/false,
                "response": "友好的确认回复"
            }
            
            **创建账本**：
            {
                "type": "create_ledger",
                "ledgerName": "新账本名",
                "response": "确认创建的友好回复"
            }
            
            **删除账本**：
            {
                "type": "delete_ledger",
                "ledgerName": "要删除的账本名",
                "response": "确认删除的询问"
            }
            
            **查看账本**：
            {
                "type": "view_ledgers",
                "response": "展示账本列表的回复"
            }
            
            **普通聊天**：
            {
                "type": "chat",
                "response": "自然、友好的聊天回复"
            }
            
            回复风格要求：
            - 🌟 友好亲切，像朋友一样
            - 😊 适当使用表情符号
            - 💡 主动提供建议和帮助
            - 🎯 简洁明了，不啰嗦
            
            只返回JSON，不要其他文字。
        """.trimIndent()
    }
    
    private suspend fun parseIntelligentResponse(response: String, userId: Long): String {
        return try {
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}") + 1
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                return "我理解你的意思，但处理时遇到了问题 🤔 能再说一遍吗？"
            }
            
            val jsonStr = response.substring(jsonStart, jsonEnd)
            val jsonObj = JSONObject(jsonStr)
            val type = jsonObj.getString("type")
            val aiResponse = jsonObj.getString("response")
            
            when (type) {
                "record" -> {
                    val recordIntent = RecordIntent(
                        amount = jsonObj.getDouble("amount"),
                        ledgerName = jsonObj.getString("ledgerName"),
                        description = jsonObj.getString("description"),
                        type = jsonObj.getString("category"),
                        isIncome = jsonObj.getBoolean("isIncome")
                    )
                    handleRecordCreation(recordIntent, userId)
                    aiResponse
                }
                "create_ledger" -> {
                    val ledgerName = jsonObj.getString("ledgerName")
                    handleLedgerCreationDirect(ledgerName, userId)
                }
                "delete_ledger" -> {
                    val ledgerName = jsonObj.getString("ledgerName")
                    handleLedgerDeletion(ledgerName, userId)
                }
                "view_ledgers" -> {
                    handleViewLedgers(userId)
                }
                "chat" -> {
                    aiResponse
                }
                else -> "我明白你的意思，让我想想怎么帮你 🤔"
            }
        } catch (e: Exception) {
            "我理解你想说什么，但处理时出了点小问题 😅 能换个方式告诉我吗？"
        }
    }
    
    private suspend fun handleLedgerCreationDirect(ledgerName: String, userId: Long): String {
        return try {
            val existingCount = ledgerDao.countLedgersByNameAndUserId(ledgerName, userId)
            if (existingCount > 0) {
                "账本《$ledgerName》已经存在啦 😊 要不换个名字？"
            } else {
                val newLedger = Ledger(
                    name = ledgerName,
                    userId = userId
                )
                ledgerDao.insert(newLedger)
                "好的！已经为你创建了《$ledgerName》账本 ✨ 现在可以开始记账了！"
            }
        } catch (e: Exception) {
            "创建账本时遇到了问题 😅 请稍后再试。"
        }
    }
    
    private suspend fun handleLedgerDeletion(ledgerName: String, userId: Long): String {
        return try {
            val ledger = ledgerDao.getLedgerByNameForUser(ledgerName, userId)
            if (ledger != null) {
                ledgerDao.delete(ledger)
                "已经删除了《$ledgerName》账本 🗑️ 相关的记录也一起清理了。"
            } else {
                "没有找到《$ledgerName》这个账本呢 🤔 要不看看其他账本？"
            }
        } catch (e: Exception) {
            "删除账本时遇到了问题 😅 请稍后再试。"
        }
    }
    
    private suspend fun handleViewLedgers(userId: Long): String {
        return try {
            val ledgers = ledgerDao.getLedgersByUserId(userId).first()
            if (ledgers.isEmpty()) {
                "你还没有创建任何账本呢 📝\n\n💡 试试说：'帮我创建一个生活费账本'"
            } else {
                "这是你的账本列表 📚\n\n" +
                ledgers.mapIndexed { index, ledger -> 
                    "${index + 1}. 《${ledger.name}》"
                }.joinToString("\n") +
                "\n\n💬 想操作哪个账本？直接告诉我就行！"
            }
        } catch (e: Exception) {
            "查看账本时遇到了问题 😅 请稍后再试。"
        }
    }
    
    private suspend fun handleRecordCreation(intent: RecordIntent, userId: Long): String {
        return try {
            val existingLedger = ledgerDao.getLedgerByNameForUser(intent.ledgerName, userId)
            
            if (existingLedger == null) {
                pendingLedgerCreation = PendingLedgerCreation(
                    ledgerName = intent.ledgerName,
                    recordIntent = intent
                )
                "我没有找到《${intent.ledgerName}》这个账本。\n\n" +
                "要为你创建这个账本吗？回复'是'或'否'。"
            } else {
                val record = Record(
                    ledgerId = existingLedger.id,
                    name = intent.description,
                    amount = intent.amount,
                    type = intent.type,
                    note = if (intent.isIncome) "收入" else "支出"
                )
                
                recordDao.insert(record)
                
                val typeText = if (intent.isIncome) "收入" else "支出"
                "✅ 记录成功！\n\n" +
                "💰 ${typeText}：${intent.amount}元\n" +
                "📝 描述：${intent.description}\n" +
                "📚 账本：《${intent.ledgerName}》\n" +
                "🏷️ 类型：${intent.type}"
            }
        } catch (e: Exception) {
            "记录时出现错误：${e.message}"
        }
    }
    
    private suspend fun handleLedgerCreation(pending: PendingLedgerCreation, userId: Long): String {
        return try {
            val newLedger = Ledger(
                name = pending.ledgerName,
                userId = userId
            )
            ledgerDao.insert(newLedger)
            
            pendingLedgerCreation = null
            
            handleRecordCreation(pending.recordIntent, userId)
        } catch (e: Exception) {
            pendingLedgerCreation = null
            "创建账本失败：${e.message}"
        }
    }
    
    private suspend fun callDoubaoAPI(prompt: String): String {
        val requestBody = JSONObject().apply {
            put("model", MODEL_NAME)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 1000)
            put("temperature", 0.7)
        }
        
        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $API_KEY")
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
                return message.getString("content")
            }
        }
        
        throw Exception("API调用失败")
    }
}

data class RecordIntent(
    val amount: Double,
    val ledgerName: String,
    val description: String,
    val type: String,
    val isIncome: Boolean
)

data class PendingLedgerCreation(
    val ledgerName: String,
    val recordIntent: RecordIntent
)