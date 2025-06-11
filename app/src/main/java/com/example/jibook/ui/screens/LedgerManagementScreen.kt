package com.example.jibook.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jibook.AnalysisActivity
import com.example.jibook.BudgetActivity
import com.example.jibook.LedgerDetailActivity
import com.example.jibook.data.AppDatabase
import com.example.jibook.models.Ledger
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerManagementScreen(
    userId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ledgerDao = remember { AppDatabase.getDatabase(context).ledgerDao() }
    val coroutineScope = rememberCoroutineScope()
    
    var ledgers by remember { mutableStateOf<List<Ledger>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newLedgerName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    
    // 加载用户的账本列表
    LaunchedEffect(userId) {
        ledgerDao.getLedgersByUserId(userId).collect {
            ledgers = it
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部操作栏
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "账本管理",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 添加账本按钮
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新建账本")
                    }
                    
                    // AI分析按钮
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, AnalysisActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        // 第99行附近 - AI分析按钮
                        Icon(
                            Icons.Default.BarChart, // 替换 Analytics
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        
                        // 第106行附近 - 预算管理按钮
                        Icon(
                            Icons.Default.AccountBalance, // 保持不变
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        
                        // 第113行附近 - 空状态图标
                        Icon(
                            Icons.Default.Book, // 替换 MenuBook
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        
                        // 第130行附近 - 预算管理按钮（如果有重复）
                        Icon(
                            Icons.Default.Wallet, // 替换 AccountBalanceWallet
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        
                        // 第152行和286行附近 - 账本卡片图标
                        Icon(
                            Icons.Default.Book, // 替换 MenuBook
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        // 第312行附近 - 查看详情按钮
                        Icon(
                            Icons.Default.Visibility, // 替换 RemoveRedEye
                            contentDescription = "查看详情"
                        )
                    }
                    
                    // 预算管理按钮
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, BudgetActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("预算管理")
                    }
                }
            }
        }
        
        // 账本列表
        if (ledgers.isEmpty()) {
            // 空状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "还没有账本",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击上方按钮创建你的第一个账本",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ledgers) { ledger ->
                    LedgerCard(
                        ledger = ledger,
                        onViewDetails = {
                            val intent = Intent(context, LedgerDetailActivity::class.java)
                            intent.putExtra("ledgerId", ledger.id)
                            intent.putExtra("ledgerName", ledger.name)
                            context.startActivity(intent)
                        },
                        onDelete = {
                            coroutineScope.launch {
                                ledgerDao.delete(ledger.id)
                            }
                        }
                    )
                }
            }
        }
    }
    
    // 添加账本对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                newLedgerName = ""
            },
            title = { Text("新建账本") },
            text = {
                Column {
                    Text("请输入账本名称：")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newLedgerName,
                        onValueChange = { newLedgerName = it },
                        placeholder = { Text("例如：日常开销") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLedgerName.isNotBlank()) {
                            isCreating = true
                            coroutineScope.launch {
                                try {
                                    // 检查账本名称是否已存在（针对当前用户）
                                    val existingCount = ledgerDao.countLedgersByNameAndUserId(newLedgerName, userId)
                                    if (existingCount > 0) {
                                        // TODO: 显示错误提示
                                    } else {
                                        val newLedger = Ledger(
                                            name = newLedgerName,
                                            userId = userId
                                        )
                                        ledgerDao.insert(newLedger)
                                        showAddDialog = false
                                        newLedgerName = ""
                                    }
                                } finally {
                                    isCreating = false
                                }
                            }
                        }
                    },
                    enabled = newLedgerName.isNotBlank() && !isCreating
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
                        showAddDialog = false
                        newLedgerName = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun LedgerCard(
    ledger: Ledger,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
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
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ledger.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "创建时间：${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ledger.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 查看详情按钮
            IconButton(onClick = onViewDetails) {
                Icon(
                    Icons.Default.RemoveRedEye, // 替换 Visibility
                    contentDescription = "查看详情"
                )
            }
            
            // 删除按钮
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除账本",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除账本") },
            text = { Text("确定要删除账本 \"${ledger.name}\" 吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}