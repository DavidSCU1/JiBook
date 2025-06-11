package com.example.jibook.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.example.jibook.models.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId ORDER BY startDate DESC")
    fun getBudgetsByLedger(ledgerId: Long): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE ledgerId = :ledgerId AND :date BETWEEN startDate AND endDate")
    suspend fun getActiveBudgetForDate(ledgerId: Long, date: Long): Budget?

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): Budget?

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
}