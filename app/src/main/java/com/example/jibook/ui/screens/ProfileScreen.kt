@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.jibook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jibook.data.AppDatabase
import com.example.jibook.models.User
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userId: Long,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val userDao = remember { AppDatabase.getDatabase(context).userDao() }
    val coroutineScope = rememberCoroutineScope()
    
    var user by remember { mutableStateOf<User?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    
    // 加载用户信息
    LaunchedEffect(userId) {
        user = userDao.getUserById(userId)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部标题
        Text(
            text = "个人中心",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // 用户信息卡片
        user?.let { currentUser ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser.username.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = currentUser.username,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "账号ID: ${currentUser.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        // 功能选项列表
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileMenuItem(
                icon = Icons.Default.Person,
                title = "修改头像",
                subtitle = "设置个性化头像",
                onClick = {
                    // TODO: 实现头像修改功能
                }
            )
            
            // 第122行附近 - 添加新账号
            ProfileMenuItem(
                icon = Icons.Default.Add, // 替换 PersonAdd
                title = "添加新账号",
                subtitle = "创建新的用户账号",
                onClick = {
                    showAddAccountDialog = true
                }
            )
            
            // 第346行附近 - 箭头图标
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight, // 替换 ChevronRight
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "应用设置",
                subtitle = "个性化设置",
                onClick = {
                    // TODO: 实现设置功能
                }
            )
            
            ProfileMenuItem(
                icon = Icons.Default.Info,
                title = "关于应用",
                subtitle = "版本信息和帮助",
                onClick = {
                    // TODO: 实现关于页面
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 退出登录按钮
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                )
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("退出登录")
            }
        }
    }
    
    // 退出登录确认对话框
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确认退出") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            userDao.setUserInactive(userId)
                            showLogoutDialog = false
                            onLogout()
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 添加新账号对话框
    if (showAddAccountDialog) {
        var newUsername by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var isCreating by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { 
                if (!isCreating) {
                    showAddAccountDialog = false
                    newUsername = ""
                    newPassword = ""
                }
            },
            title = { Text("添加新账号") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("用户名") },
                        enabled = !isCreating,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("密码") },
                        enabled = !isCreating,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUsername.isNotBlank() && newPassword.isNotBlank()) {
                            isCreating = true
                            coroutineScope.launch {
                                try {
                                    val existingUser = userDao.getUserByUsername(newUsername)
                                    if (existingUser != null) {
                                        // 用户名已存在
                                        // TODO: 显示错误提示
                                    } else {
                                        // 创建新用户
                                        val hashedPassword = java.security.MessageDigest.getInstance("SHA-256")
                                            .digest(newPassword.toByteArray())
                                            .joinToString("") { "%02x".format(it) }
                                        
                                        val newUser = User(
                                            username = newUsername,
                                            password = hashedPassword,
                                            isActive = false
                                        )
                                        userDao.insertUser(newUser)
                                        
                                        showAddAccountDialog = false
                                        newUsername = ""
                                        newPassword = ""
                                    }
                                } finally {
                                    isCreating = false
                                }
                            }
                        }
                    },
                    enabled = !isCreating && newUsername.isNotBlank() && newPassword.isNotBlank()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("创建")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddAccountDialog = false
                        newUsername = ""
                        newPassword = ""
                    },
                    enabled = !isCreating
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}