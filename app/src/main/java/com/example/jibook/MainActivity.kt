package com.example.jibook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.jibook.data.AppDatabase
import com.example.jibook.ui.screens.*
import com.example.jibook.ui.theme.JiBookTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var userDao: com.example.jibook.data.UserDao
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        userDao = AppDatabase.getDatabase(this).userDao()
        
        lifecycleScope.launch {
            val currentUser = userDao.getCurrentUser()
            if (currentUser == null) {
                startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                finish()
            } else {
                setContent {
                    JiBookTheme {
                        MainScreen(currentUser.id)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(userId: Long) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                // 第52行附近
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Book, contentDescription = null) }, // 替换 MenuBook
                    label = { Text("账本管理") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) }, // 替换 SmartToy
                    label = { Text("AI聊天") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("个人中心") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> LedgerManagementScreen(
                userId = userId,
                modifier = Modifier.padding(paddingValues)
            )
            1 -> AIChatScreen(
                userId = userId,
                modifier = Modifier.padding(paddingValues)
            )
            2 -> ProfileScreen(
                userId = userId,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}