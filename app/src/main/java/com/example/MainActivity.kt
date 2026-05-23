package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.CheckInRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.WebDavStatus
import com.example.ui.components.AnalyticsSection
import com.example.ui.components.CalendarView
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.LogsSection
import com.example.ui.components.StatsSection
import com.example.ui.components.SyncSection
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: CheckInRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room DB
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "star_habits_db"
        ).fallbackToDestructiveMigration().build()

        repository = CheckInRepository(
            context = applicationContext,
            checkInDao = database.checkInDao(),
            logEntryDao = database.logEntryDao(),
            configEntryDao = database.configEntryDao()
        )

        val viewModel: MainViewModel by viewModels {
            MainViewModelFactory(repository)
        }

        enableEdgeToEdge()

        setContent {
            // Theme state observing in view model
            val userDarkModeSec by viewModel.isDarkMode.collectAsState()
            val useDark = when (userDarkModeSec) {
                null -> isSystemInDarkTheme()
                else -> userDarkModeSec == true
            }

            MyApplicationTheme(darkTheme = useDark) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // UI states
    val stats by viewModel.statsState.collectAsState()
    val checkIns by viewModel.checkInsState.collectAsState()
    val logs by viewModel.logsState.collectAsState()
    
    // Calendar view dates
    val calendarYear by viewModel.calendarYear.collectAsState()
    val calendarMonth by viewModel.calendarMonth.collectAsState()
    
    // WebDAV
    val webdavUrl by viewModel.webdavUrl.collectAsState()
    val webdavUser by viewModel.webdavUser.collectAsState()
    val webdavPassword by viewModel.webdavPassword.collectAsState()
    val webdavFileName by viewModel.webdavFileName.collectAsState()
    val syncStatus by viewModel.webdavSyncStatus.collectAsState()

    // Dialog state
    val makeUpConfirmDate by viewModel.showMakeUpConfirm.collectAsState()
    val deleteConfirmDate by viewModel.showDeleteConfirm.collectAsState()

    // Navigation Tab state: 0: Calendar, 1: Stats, 2: Settings
    var currentTab by remember { mutableStateOf(0) }

    // Logs Overlay state
    var showHistoryLogs by remember { mutableStateOf(false) }

    // Floating Tab Bar state
    val isFloatingBar by viewModel.isFloatingBottomBar.collectAsState()

    // Logs retention state
    val maxLogsLimit by viewModel.maxLogsLimit.collectAsState()

    // Theme state
    val forceDarkState by viewModel.isDarkMode.collectAsState()

    // Confetti celebration state trigger
    var activeConfettiTrigger by remember { mutableStateOf<Any?>(null) }

    // Listen to ViewModel events for snackbar and confetti triggering
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            if (msg != null) {
                snackbarHostState.showSnackbar(msg)
                viewModel.snackbarMessage.value = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.confettiTrigger.collect {
            activeConfettiTrigger = System.currentTimeMillis() // Trigger confetti
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "迹",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    // Logs button only, with no badges or numbers displaying
                    IconButton(
                        onClick = { showHistoryLogs = true },
                        modifier = Modifier.testTag("top_logs_toggle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "打卡痕迹历史日志",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            if (!isFloatingBar) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "日历打卡"
                            )
                        },
                        label = { Text("打卡", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("tab_calendar")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.InsertChartOutlined,
                                contentDescription = "打卡统计"
                            )
                        },
                        label = { Text("统计", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("tab_stats")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "系统设置"
                            )
                        },
                        label = { Text("设置", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("tab_sync")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Crossfade transitions
            androidx.compose.animation.Crossfade(
                targetState = currentTab,
                modifier = Modifier.fillMaxSize()
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // Calendar Tab Screen
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Stats Dashboard Header
                            StatsSection(
                                thisMonthCount = stats.thisMonthCount,
                                totalCount = stats.totalCount,
                                maxStreakDays = stats.maxStreakDays
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Interactive Custom Calendar Module
                            CalendarView(
                                year = calendarYear,
                                month = calendarMonth,
                                checkIns = checkIns,
                                onDayClick = { date -> viewModel.showMakeUpConfirm.value = date },
                                onDayLongClick = { date -> viewModel.showDeleteConfirm.value = date },
                                onNavigateMonth = { delta -> viewModel.navigateMonth(delta) },
                                onMonthYearSelect = { targetYear, targetMonth -> viewModel.setYearMonth(targetYear, targetMonth) },
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Bottom helper instructions
                            Text(
                                text = "💡 小提示: 点击空白日期补签打卡，长按绿色星斑日期删除打卡",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                            )

                            if (isFloatingBar) {
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }
                    }
                    1 -> {
                        // Advanced analytics tab screen
                        AnalyticsSection(
                            checkIns = checkIns,
                            isFloatingBarEnabled = isFloatingBar,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        // Configuration Settings & Backup/Restore Panel
                        SyncSection(
                            url = webdavUrl,
                            user = webdavUser,
                            pass = webdavPassword,
                            remoteFile = webdavFileName,
                            status = syncStatus,
                            isFloatingBarEnabled = isFloatingBar,
                            themeMode = forceDarkState,
                            onThemeChange = { viewModel.changeTheme(it) },
                            maxLogsLimit = maxLogsLimit,
                            onMaxLogsLimitChange = { viewModel.updateMaxLogsLimit(it) },
                            onUrlChange = { viewModel.updateWebDAVUrl(it) },
                            onUserChange = { viewModel.updateWebDAVUser(it) },
                            onPassChange = { viewModel.updateWebDAVPassword(it) },
                            onFileNameChange = { viewModel.updateWebDAVFileName(it) },
                            onTestConnection = { viewModel.testWebDAVConnection() },
                            onBackupToWebDAV = { viewModel.syncBackupToWebDAV() },
                            onRestoreFromWebDAV = { viewModel.syncRestoreFromWebDAV() },
                            onExportLocalBackup = { viewModel.exportBackup() },
                            onImportLocalBackup = { uri -> viewModel.importBackup(context, uri) },
                            onClearAllData = { viewModel.clearAllData() },
                            onToggleFloatingBar = { enabled -> viewModel.toggleFloatingBottomBar(enabled) }
                        )
                    }
                }
            }

            // Floating Navigation Bar Overlay Overlay Dock (when enabled - compact 220.dp minimalist capsule)
            if (isFloatingBar) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .navigationBarsPadding()
                        .width(240.dp)
                        .testTag("floating_dock"),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 0: 打卡
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(if (currentTab == 0) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .clickable { currentTab = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "打卡",
                                    tint = if (currentTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "打卡",
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (currentTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Tab 1: 统计
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(if (currentTab == 1) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .clickable { currentTab = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.InsertChartOutlined,
                                    contentDescription = "统计",
                                    tint = if (currentTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "统计",
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (currentTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Tab 2: 设置
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(if (currentTab == 2) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .clickable { currentTab = 2 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "设置",
                                    tint = if (currentTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "设置",
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == 2) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (currentTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // --- Click/Make-up Action Double Confirmation Dialog with Remark Input ---
            if (makeUpConfirmDate != null) {
                val date = makeUpConfirmDate!!
                var remarkText by remember { mutableStateOf("") }
                
                val todayStr = remember {
                    val today = java.util.Calendar.getInstance()
                    String.format(java.util.Locale.US, "%04d-%02d-%02d", 
                        today.get(java.util.Calendar.YEAR), 
                        today.get(java.util.Calendar.MONTH) + 1, 
                        today.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                }
                val isToday = date == todayStr
                val isBeforeToday = date < todayStr

                AlertDialog(
                    onDismissRequest = { viewModel.showMakeUpConfirm.value = null },
                    title = {
                        Text(
                            text = if (isToday) "今日打卡" else if (isBeforeToday) "确认打卡补签" else "确认提前规划",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = if (isToday) 
                                    "确认点亮今日 ($date) 的足迹印记吗？打卡后，日历上将亮起耀眼的守护星标。" 
                                else if (isBeforeToday)
                                    "您想在过去的日期日期 $date 进行补充签名吗？系统将会记录您的打卡，并自动列入连续天数中。"
                                else
                                    "确认写入未来日期 $date 的打卡足迹么？规划未来的坚持足迹同样意义非凡。",
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = remarkText,
                                onValueChange = { remarkText = it },
                                label = { Text("添加打卡备注 (可选)") },
                                placeholder = { Text("例如：完成了半程慢跑、背了新单词...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_checkin_remark"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.checkInDate(date, isMakeUp = !isToday, note = remarkText)
                                viewModel.showMakeUpConfirm.value = null
                            },
                            modifier = Modifier.testTag("dialog_makeup_confirm")
                        ) {
                            Text(
                                text = if (isToday) "立即打卡" else "确定补签", 
                                color = MaterialTheme.colorScheme.primary, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.showMakeUpConfirm.value = null },
                            modifier = Modifier.testTag("dialog_makeup_cancel")
                        ) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // --- History Logs Modal Overlay ---
            if (showHistoryLogs) {
                AlertDialog(
                    onDismissRequest = { showHistoryLogs = false },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "历史打卡痕迹",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { showHistoryLogs = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    text = {
                        Box(modifier = Modifier.height(380.dp).fillMaxWidth()) {
                            LogsSection(
                                logs = logs,
                                onClearLogs = { 
                                    viewModel.clearAllData() 
                                    showHistoryLogs = false
                                }
                            )
                        }
                    },
                    confirmButton = {},
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // --- Delete Action Double Confirmation Dialog ---
            if (deleteConfirmDate != null) {
                val date = deleteConfirmDate!!
                AlertDialog(
                    onDismissRequest = { viewModel.showDeleteConfirm.value = null },
                    title = {
                        Text(
                            text = "确认删除打卡",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    text = {
                        Text(
                            text = "您确想要删除日期 $date 的打卡记录吗？本操作将移除对应的日历星光徽标，此天不再记为您的打卡天数，并在历史日志中留有撤销项记录。",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteCheckInDate(date)
                                viewModel.showDeleteConfirm.value = null
                            },
                            modifier = Modifier.testTag("dialog_delete_confirm")
                        ) {
                            Text("确定删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.showDeleteConfirm.value = null },
                            modifier = Modifier.testTag("dialog_delete_cancel")
                        ) {
                            Text("保留记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // Confetti Layer: Placed here so elements fall directly on top of all Scaffold contents
            ConfettiOverlay(
                trigger = activeConfettiTrigger,
                onFinished = { activeConfettiTrigger = null }
            )
        }
    }
}
