package com.example.jibook.data

import androidx.room.*
import com.example.jibook.models.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?
    
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?
    
    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    suspend fun getCurrentUser(): User?
    
    @Query("UPDATE users SET isActive = 0")
    suspend fun clearCurrentUser()
    
    @Query("UPDATE users SET isActive = 1 WHERE id = :userId")
    suspend fun setCurrentUser(userId: Long)
    
    @Query("UPDATE users SET isActive = 0 WHERE id = :userId")
    suspend fun setUserInactive(userId: Long)
    
    @Insert
    suspend fun insert(user: User): Long
    
    @Insert
    suspend fun insertUser(user: User): Long
    
    @Update
    suspend fun update(user: User)
    
    @Delete
    suspend fun delete(user: User)
}