package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WebDavStatus

enum class SubSetting {
    WEBDAV,
    APPEARANCE,
    DATA,
    DANGER,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSection(
    url: String,
    user: String,
    pass: String,
    remoteFile: String,
    status: WebDavStatus,
    isFloatingBarEnabled: Boolean,
    themeMode: Boolean?,
    onThemeChange: (Boolean?) -> Unit,
    maxLogsLimit: String,
    onMaxLogsLimitChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onFileNameChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onBackupToWebDAV: () -> Unit,
    onRestoreFromWebDAV: () -> Unit,
    onExportLocalBackup: () -> Unit,
    onImportLocalBackup: (Uri) -> Unit,
    onClearAllData: () -> Unit,
    onToggleFloatingBar: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    
    // Active sub-setting screen state (null means main settings menu)
    var activeSubSetting by remember { mutableStateOf<SubSetting?>(null) }

    // File Picker for local import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportLocalBackup(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeSubSetting == null) {
            // ==================== MAIN SETTINGS MENU ====================
            
            // Header Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "系统设置",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "配置云同步、外观布局、操作日志及数据管理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu Items List (M3 Cards with ripple feedback)
            
            // 1. WebDAV Sync Row
            SettingMenuCard(
                title = "云同步配置 (WebDAV)",
                description = "配置坚果云、Nextcloud等，开启安全合并漫游",
                icon = Icons.Default.CloudSync,
                tag = "menu_webdav",
                onClick = { activeSubSetting = SubSetting.WEBDAV }
            )

            // 2. Appearance Row
            SettingMenuCard(
                title = "界面与外观 (Appearance)",
                description = "修改深浅色主题模式，切换极简悬浮及传统底部栏",
                icon = Icons.Default.Palette,
                tag = "menu_appearance",
                onClick = { activeSubSetting = SubSetting.APPEARANCE }
            )

            // 3. Data & Limit Row
            SettingMenuCard(
                title = "数据备份与限额 (Data & Capacity)",
                description = "本地 JSON 导出与导入，配置历史操作日志保留空间",
                icon = Icons.Default.Storage,
                tag = "menu_data",
                onClick = { activeSubSetting = SubSetting.DATA }
            )

            // 4. Danger Zone Row
            SettingMenuCard(
                title = "数据清理红区 (Danger Zone)",
                description = "清空与擦除本地所有打卡数据及操作日志",
                icon = Icons.Default.DeleteSweep,
                color = MaterialTheme.colorScheme.error,
                tag = "menu_danger",
                onClick = { activeSubSetting = SubSetting.DANGER }
            )

            // 5. About Screen Row
            SettingMenuCard(
                title = "关于 迹 (About App)",
                description = "查看系统信息、开发者声明及 GitHub 源代码库",
                icon = Icons.Default.Info,
                tag = "menu_about",
                onClick = { activeSubSetting = SubSetting.ABOUT }
            )

        } else {
            // ==================== SUB-SETTING DETAIL SCREEN ====================
            
            // Back button and path navigator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { activeSubSetting = null }
                    .padding(vertical = 12.dp, horizontal = 4.dp)
                    .testTag("submenu_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "返回主设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (activeSubSetting!!) {
                SubSetting.WEBDAV -> {
                    // --- WebDAV Secondary Screen ---
                    Text(
                        text = "WebDAV 云备份同步",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "将打卡足迹存储配置关联到您的第三方网盘中。即使发生设备损坏或丢失，足迹星闪数据依然能在新机安全恢复。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        label = { Text("WebDAV 服务器 URL 端点") },
                        placeholder = { Text("如 https://dav.jianguoyun.com/dav/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("webdav_url_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = user,
                        onValueChange = onUserChange,
                        label = { Text("WebDAV 账户账号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("webdav_user_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = pass,
                        onValueChange = onPassChange,
                        label = { Text("专有 App 单独应用凭证(非登录密码)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val viewIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = viewIcon, contentDescription = "显隐密码")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("webdav_pass_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = remoteFile,
                        onValueChange = onFileNameChange,
                        label = { Text("云端存储的 JSON 文件名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("webdav_file_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Synchronization results message
                    AnimatedVisibility(visible = status !is WebDavStatus.Idle) {
                        val messageText = when (status) {
                            is WebDavStatus.Loading -> status.msg
                            is WebDavStatus.Success -> "同步网卡反馈: " + status.msg
                            is WebDavStatus.Error -> "错误汇报: " + status.msg
                            else -> ""
                        }
                        val labelBgColor = when (status) {
                            is WebDavStatus.Loading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            is WebDavStatus.Success -> MaterialTheme.colorScheme.primaryContainer
                            is WebDavStatus.Error -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val labelOnColor = when (status) {
                            is WebDavStatus.Loading -> MaterialTheme.colorScheme.onPrimaryContainer
                            is WebDavStatus.Success -> MaterialTheme.colorScheme.onPrimaryContainer
                            is WebDavStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(labelBgColor)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (status is WebDavStatus.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = labelOnColor
                                )
                            } else {
                                Icon(
                                    imageVector = if (status is WebDavStatus.Success) Icons.Default.CloudDone else Icons.Default.Lock,
                                    contentDescription = "提示",
                                    tint = labelOnColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = messageText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = labelOnColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestConnection,
                            modifier = Modifier.fillMaxWidth().testTag("test_conn_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = "测试",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("连接安全测试")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onBackupToWebDAV,
                                modifier = Modifier.weight(1f).testTag("webdav_backup_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "备份",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("备份至云端", fontSize = 11.sp)
                            }

                            Button(
                                onClick = onRestoreFromWebDAV,
                                modifier = Modifier.weight(1f).testTag("webdav_restore_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "拉取",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("拉取并合并", fontSize = 11.sp)
                            }
                        }
                    }
                }

                SubSetting.APPEARANCE -> {
                    // --- Theme / Visual Appearance Settings Screen ---
                    Text(
                        text = "界面与外观偏好",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "在此调整「迹」的深浅主题倾向，并自由选择底栏样式布局以融入您个人的壁纸外观和交互习惯。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // A. Theme Switcher inside Appearance (Moved from the old Top Bar as requested)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "外观色彩模式",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val currentMode = themeMode
                                
                                // Alternative choices: System default, Light mode, Dark mode
                                AppearanceOptionButton(
                                    text = "跟随系统",
                                    selected = currentMode == null,
                                    onClick = { onThemeChange(null) },
                                    modifier = Modifier.weight(1f)
                                )

                                AppearanceOptionButton(
                                    text = "明亮日光",
                                    selected = currentMode == false,
                                    onClick = { onThemeChange(false) },
                                    modifier = Modifier.weight(1f)
                                )

                                AppearanceOptionButton(
                                    text = "深邃夜空",
                                    selected = currentMode == true,
                                    onClick = { onThemeChange(true) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // B. Custom Floating Bottom Bar preference
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("floating_bar_settings_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "现代悬浮底栏样式",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "开启后，底部导航栏将切换为富有跃动感的极简悬浮胶囊形状。",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isFloatingBarEnabled,
                                onCheckedChange = onToggleFloatingBar,
                                modifier = Modifier.testTag("floating_bar_switch")
                            )
                        }
                    }
                }

                SubSetting.DATA -> {
                    // --- General Backups & Limits Settings Screen ---
                    Text(
                        text = "数据备份与限制",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "导入或导出设备上的离线 JSON 备份，并设定日志的最大驻留额度以限制其对系统空间的长时间慢速霸占。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Local Import/Export Card
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("local_backup_settings_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "离线数据备份文件",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "您可以导出当前的全部数据，并在发生系统重置或更换新手机时通过本地文件导入进行数据恢复。",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onExportLocalBackup,
                                    modifier = Modifier.weight(1f).testTag("export_local_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = "导出",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("导出备份", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { importLauncher.launch("application/json") },
                                    modifier = Modifier.weight(1f).testTag("import_local_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "导入",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("导入本地文件", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Log count Limit Card (As requested: 同时可以设置日志最大能占用多少内存/条数)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "历史打卡日志存储额度",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "当日志存储条数超过所设限额后，系统会在您产生新动作的同时静默裁撤并清理更久远的冗余垃圾日志。减少内存驻留，保证数据库高效检索。",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Flow-styled Selectors for Max Limit sizes
                            val limits = listOf(
                                "50" to "50条\n(~15KB)",
                                "100" to "100条\n(~30KB)",
                                "500" to "500条\n(~150KB)",
                                "1000" to "1000条\n(~300KB)",
                                "unlimited" to "无限制\n(不封顶)"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                limits.forEach { (key, display) ->
                                    val isSelected = maxLogsLimit == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                            )
                                            .clickable { onMaxLogsLimitChange(key) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = display,
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp,
                                            textAlign = TextAlign.Center,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SubSetting.DANGER -> {
                    // --- Danger Zone Secondary Screen ---
                    Text(
                        text = "数据清理红区",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "此处的清空按钮旨在对打卡软件的核心数据做彻底的重置擦除。按下之后，您在这个地方所有的坚持、足迹、备注以及日志文件都会随风消散，没有历史备份绝不可挽回。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("danger_zone_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "警报",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "确认清空数据吗？",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            if (!showClearDataConfirm) {
                                OutlinedButton(
                                    onClick = { showClearDataConfirm = true },
                                    modifier = Modifier.fillMaxWidth().testTag("danger_delete_init"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("清空本地全部数据", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onClearAllData()
                                            showClearDataConfirm = false
                                        },
                                        modifier = Modifier.weight(1f).testTag("danger_delete_confirm"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("确定擦除(不可恢复)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showClearDataConfirm = false },
                                        modifier = Modifier.weight(1f).testTag("danger_delete_cancel"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("取消", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                SubSetting.ABOUT -> {
                    // --- About Ji Secondary Screen ---
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("about_app_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "迹 · Ji",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )

                            Text(
                                text = "留下点滴足迹，终能灿若繁星",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "「迹」是一款注重离线私密安全打卡的伴侣产品。我们始终相信唯有数据自持方能承载宝贵的回忆痕迹。通过简洁现代的日历与纯粹的数据统计指标，默默陪伴您记录点滴积累。",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Interactive Git Link Layout
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .clickable {
                                        try {
                                            val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Yuyaniel/Ji"))
                                            context.startActivity(openUrlIntent)
                                        } catch (e: Exception) {
                                            // Fallback
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AlternateEmail,
                                    contentDescription = "GitHub 源码链接",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GitHub 开源地址: Yuyaniel/Ji",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "前往",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            Text(
                                text = "版本 v1.1.0 • 离线优先架构",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // --- Generous Spacing for Floating Bar overlay overlapping avoidance ---
        // Always place this bottom spacer so that content at the very bottom can easily scroll above the floating dock bar.
        if (isFloatingBarEnabled) {
            Spacer(modifier = Modifier.height(90.dp))
        } else {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun SettingMenuCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "详情",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AppearanceOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
