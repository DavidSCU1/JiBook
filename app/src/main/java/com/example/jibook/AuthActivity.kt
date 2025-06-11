package com.example.jibook

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.jibook.data.AppDatabase
import com.example.jibook.models.User
import com.example.jibook.ui.theme.JiBookTheme
import kotlinx.coroutines.launch
import java.security.MessageDigest

class AuthActivity : ComponentActivity() {
    private lateinit var userDao: com.example.jibook.data.UserDao
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        userDao = AppDatabase.getDatabase(this).userDao()
        
        setContent {
            JiBookTheme {
                AuthScreen(
                    onLoginSuccess = {
                        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                        finish()
                    },
                    userDao = userDao
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    userDao: com.example.jibook.data.UserDao
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "登录" else "注册",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    // 第101行附近 - 密码字段
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                    )
                }
            }
        )
        
        if (!isLoginMode) {
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        // 第124行附近 - 确认密码字段
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (isLoginMode) {
                    // 登录逻辑
                    if (username.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "请填写完整信息", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    
                    (context as ComponentActivity).lifecycleScope.launch {
                        try {
                            val user = userDao.getUserByUsername(username)
                            if (user != null && user.password == hashPassword(password)) {
                                userDao.clearCurrentUser()
                                userDao.setCurrentUser(user.id)
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "用户名或密码错误", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "登录失败：${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    // 注册逻辑
                    if (username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                        Toast.makeText(context, "请填写完整信息", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    if (password != confirmPassword) {
                        Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    if (password.length < 6) {
                        Toast.makeText(context, "密码长度至少6位", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    
                    (context as ComponentActivity).lifecycleScope.launch {
                        try {
                            val existingUser = userDao.getUserByUsername(username)
                            if (existingUser != null) {
                                Toast.makeText(context, "用户名已存在", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            val newUser = User(
                                username = username,
                                password = hashPassword(password)
                            )
                            
                            val userId = userDao.insert(newUser)
                            userDao.clearCurrentUser()
                            userDao.setCurrentUser(userId)
                            
                            Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } catch (e: Exception) {
                            Toast.makeText(context, "注册失败：${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text(if (isLoginMode) "登录" else "注册")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = { isLoginMode = !isLoginMode }
        ) {
            Text(if (isLoginMode) "没有账号？点击注册" else "已有账号？点击登录")
        }
    }
}

fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(password.toByteArray())
    return hash.fold("") { str, it -> str + "%02x".format(it) }
}