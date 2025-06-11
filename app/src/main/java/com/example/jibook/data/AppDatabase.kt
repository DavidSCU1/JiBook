package com.example.jibook.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.jibook.models.Ledger
import com.example.jibook.models.Record
import com.example.jibook.models.Budget
import com.example.jibook.models.User
import com.example.jibook.models.ChatMessage

@Database(
    entities = [Ledger::class, Record::class, Budget::class, User::class, ChatMessage::class], 
    version = 3, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun recordDao(): RecordDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userDao(): UserDao
    abstract fun chatMessageDao(): ChatMessageDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jibook_database"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建用户表
                database.execSQL("""
                    CREATE TABLE users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL,
                        password TEXT NOT NULL,
                        avatarPath TEXT,
                        createdAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 为现有表添加userId字段
                database.execSQL("ALTER TABLE ledgers ADD COLUMN userId INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE budgets ADD COLUMN userId INTEGER NOT NULL DEFAULT 1")
                
                // 创建聊天消息表
                database.execSQL("""
                    CREATE TABLE chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        isFromUser INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        messageType TEXT NOT NULL DEFAULT 'TEXT'
                    )
                """)
            }
        }
    }
}