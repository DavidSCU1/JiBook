package com.example.jibook.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.example.jibook.models.Ledger
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledgers WHERE userId = :userId ORDER BY name ASC")
    fun getLedgersByUserId(userId: Long): Flow<List<Ledger>>
    
    @Query("SELECT * FROM ledgers ORDER BY name ASC")
    fun getAllLedgers(): Flow<List<Ledger>>
    
    @Query("SELECT * FROM ledgers WHERE id = :id")
    suspend fun getLedgerById(id: Long): Ledger?
    
    @Query("SELECT * FROM ledgers WHERE name = :name AND userId = :userId LIMIT 1")
    suspend fun getLedgerByNameForUser(name: String, userId: Long): Ledger?
    
    @Query("SELECT COUNT(*) FROM ledgers WHERE name = :name AND userId = :userId")
    suspend fun countLedgersByNameAndUserId(name: String, userId: Long): Int
    
    @Query("SELECT COUNT(*) FROM ledgers WHERE name = :name")
    suspend fun countLedgersByName(name: String): Int
    
    @Insert
    suspend fun insert(ledger: Ledger): Long
    
    @Update
    suspend fun update(ledger: Ledger)
    
    @Delete
    suspend fun delete(ledger: Ledger)
    
    @Query("DELETE FROM ledgers WHERE id = :id")
    suspend fun delete(id: Long)
}