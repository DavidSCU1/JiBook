package com.example.jibook.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.example.jibook.models.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Insert
    suspend fun insert(record: Record): Long

    @Query("SELECT * FROM records WHERE ledgerId = :ledgerId ORDER BY creationDate DESC")
    fun getRecordsForLedger(ledgerId: Long): Flow<List<Record>>

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun delete(id: Long)
    
    // 新增：获取所有记录用于分析
    @Query("SELECT * FROM records ORDER BY creationDate DESC")
    fun getAllRecords(): Flow<List<Record>>
    
    // 新增：按类型分组统计
    @Query("SELECT type, SUM(amount) as total FROM records WHERE amount > 0 GROUP BY type")
    suspend fun getIncomeByType(): List<TypeAmount>
    
    @Query("SELECT type, SUM(amount) as total FROM records WHERE amount < 0 GROUP BY type")
    suspend fun getExpenseByType(): List<TypeAmount>
    
    // 新增：按月份统计
    @Query("SELECT strftime('%Y-%m', datetime(creationDate/1000, 'unixepoch')) as month, SUM(amount) as total FROM records WHERE amount > 0 GROUP BY month ORDER BY month DESC LIMIT 6")
    suspend fun getMonthlyIncome(): List<MonthlyAmount>
    
    @Query("SELECT strftime('%Y-%m', datetime(creationDate/1000, 'unixepoch')) as month, SUM(-amount) as total FROM records WHERE amount < 0 GROUP BY month ORDER BY month DESC LIMIT 6")
    suspend fun getMonthlyExpense(): List<MonthlyAmount>
}

// 数据类
data class TypeAmount(
    val type: String,
    val total: Double
)

data class MonthlyAmount(
    val month: String,
    val total: Double
)