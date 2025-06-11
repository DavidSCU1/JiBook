package com.example.jibook.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "records",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Ledger::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ledgerId"])] // 为 ledgerId 列创建索引
)
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val name: String,
    val amount: Double,
    val type: String,
    val note: String = "",
    val creationDate: Long = System.currentTimeMillis()
)