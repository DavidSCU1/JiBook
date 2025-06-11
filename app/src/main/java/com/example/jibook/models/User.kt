package com.example.jibook.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val password: String, // 实际项目中应该加密存储
    val avatarPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(), // 确保这个字段存在
    val isActive: Boolean = false // 当前登录状态
)