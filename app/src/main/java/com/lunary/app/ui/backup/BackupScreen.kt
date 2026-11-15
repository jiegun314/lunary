package com.lunary.app.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.data.backup.BackupArchive
import com.lunary.app.data.backup.RestoreMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showExportPassword by remember { mutableStateOf(false) }
    var showImportPassword by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showDriveHelp by remember { mutableStateOf(false) }
    var pendingPassphrase by remember { mutableStateOf<CharArray?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val passphrase = pendingPassphrase
        if (uri != null && passphrase != null) viewModel.export(uri, passphrase)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            showImportPassword = true
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备份与恢复") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            SectionCard("本地加密备份") {
                Text(
                    "导出经过密码加密的 .lunary 文件（包含事项、分类、标签、提醒和附件）。密码不会保存在本机，丢失后无法恢复。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showExportPassword = true },
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f)
                    ) { Text("导出备份") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f)
                    ) { Text("导入备份") }
                }
                Text(
                    "恢复前会自动生成一份当前数据的本地快照（auto_backups，最多保留 5 份）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard("Google Drive 备份") {
                Text(
                    "云端备份使用 drive.appdata 隐藏目录，只允许本应用访问，文件不会出现在 Drive 文件列表中。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Google Drive 上传/恢复尚未接入：需要 OAuth 客户端 ID 与 Drive API 实现。当前版本仅支持本地加密备份。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { showDriveHelp = true }) { Text("查看配置说明") }
            }

            SectionCard("安全说明") {
                Text(
                    "• 备份使用 PBKDF2-HMAC-SHA256（210,000 次）派生密钥，AES-256-GCM 认证加密\n" +
                        "• 备份不包含 PIN、Keystore 密钥或 OAuth 令牌\n" +
                        "• 恢复会先完整校验备份，密码错误或文件损坏时不会修改本机数据",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showExportPassword) {
        BackupPasswordDialog(
            title = "设置备份密码",
            requireConfirm = true,
            onDismiss = { showExportPassword = false },
            onConfirm = { passphrase ->
                pendingPassphrase = passphrase
                showExportPassword = false
                val name = "lunary-backup-" +
                    DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(
                        java.time.LocalDateTime.now()
                    ) + ".lunary"
                exportLauncher.launch(name)
            }
        )
    }
    if (showImportPassword) {
        BackupPasswordDialog(
            title = "输入备份密码",
            requireConfirm = false,
            onDismiss = { showImportPassword = false; pendingUri = null },
            onConfirm = { passphrase ->
                val uri = pendingUri
                showImportPassword = false
                if (uri != null) viewModel.inspect(uri, passphrase)
            }
        )
    }
    state.preview?.let { preview ->
        RestorePreviewDialog(
            archive = preview,
            onDismiss = { viewModel.dismissPreview() },
            onMerge = { viewModel.confirmRestore(RestoreMode.MERGE) },
            onReplace = { viewModel.confirmRestore(RestoreMode.REPLACE) }
        )
    }
    if (showDriveHelp) {
        AlertDialog(
            onDismissRequest = { showDriveHelp = false },
            title = { Text("Google Drive 配置") },
            text = {
                Text(
                    "1. 在 Google Cloud Console 创建项目并启用 Google Drive API\n" +
                        "2. 创建 Android 类型 OAuth 客户端，填写包名 com.lunary.app 与签名 SHA-1\n" +
                        "3. 在 local.properties 配置 lunary.drive.clientId，重新构建\n" +
                        "4. 在 OAuth 同意屏幕添加测试账号\n\n" +
                        "未配置前不会显示虚假的上传状态。详细步骤见 README。"
                )
            },
            confirmButton = { TextButton(onClick = { showDriveHelp = false }) { Text("知道了") } }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun BackupPasswordDialog(
    title: String,
    requireConfirm: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = password.length >= 8 && (!requireConfirm || password == confirm)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码（至少 8 位）") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (requireConfirm) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("再次输入密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
                Text(
                    "密码仅用于本次备份，遗忘后无法解密，请自行妥善保存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onConfirm(password.toCharArray())
                    password = ""
                    confirm = ""
                }
            ) { Text(if (requireConfirm) "导出" else "继续") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun RestorePreviewDialog(
    archive: BackupArchive,
    onDismiss: () -> Unit,
    onMerge: () -> Unit,
    onReplace: () -> Unit
) {
    val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(archive.manifest.createdAt))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("恢复预览") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("备份时间：$time")
                Text("应用版本：${archive.manifest.appVersion}")
                Text("事项 ${archive.eventCount} · 分类 ${archive.categoryCount} · 标签 ${archive.tagCount}")
                Text("提醒 ${archive.reminderCount} · 附件 ${archive.attachmentCount}")
                Text(
                    "合并恢复会保留本机数据，并把备份内容作为副本导入（标题/名称追加“恢复副本”）；替换会清空本机当前数据。两种方式都会先自动保存当前快照。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onMerge) { Text("合并恢复") }
                TextButton(
                    onClick = onReplace,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("替换本机数据") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

