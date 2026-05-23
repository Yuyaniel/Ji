package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CheckIn
import com.example.data.CheckInRepository
import com.example.data.LogEntry
import com.example.data.WebDavResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(private val repository: CheckInRepository) : ViewModel() {

    // Theme mode toggles
    val isDarkMode = MutableStateFlow<Boolean?>(null) // null = system default

    // Calendar Year/Month Navigation
    val calendarYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val calendarMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0-11

    // Selected Day Actions
    val showMakeUpConfirm = MutableStateFlow<String?>(null) // Date string of day to confirm checkin
    val showDeleteConfirm = MutableStateFlow<String?>(null) // Date string of day to confirm deletion

    // WebDAV states
    val webdavUrl = MutableStateFlow("")
    val webdavUser = MutableStateFlow("")
    val webdavPassword = MutableStateFlow("")
    val webdavFileName = MutableStateFlow("star_habits_backup.json")
    val webdavSyncStatus = MutableStateFlow<WebDavStatus>(WebDavStatus.Idle)

    // Confetti Animation Trigger
    private val _confettiTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val confettiTrigger: SharedFlow<Unit> = _confettiTrigger.asSharedFlow()

    // Status UI messages
    val snackbarMessage = MutableStateFlow<String?>(null)

    // Floating Bottom Bar Style preference
    val isFloatingBottomBar = MutableStateFlow(false)

    // Log size/retention configuration setting
    val maxLogsLimit = MutableStateFlow("unlimited")

    init {
        // Load configurations
        viewModelScope.launch {
            webdavUrl.value = repository.getConfig(CheckInRepository.KEY_WEBDAV_URL) ?: ""
            webdavUser.value = repository.getConfig(CheckInRepository.KEY_WEBDAV_USER) ?: ""
            webdavPassword.value = repository.getConfig(CheckInRepository.KEY_WEBDAV_PASSWORD) ?: ""
            webdavFileName.value = repository.getConfig(CheckInRepository.KEY_WEBDAV_REMOTE_FILE) ?: "star_habits_backup.json"
            
            val isFloating = repository.getConfig("floating_bottom_bar") ?: "false"
            isFloatingBottomBar.value = isFloating == "true"

            val maxLimit = repository.getConfig("max_logs_limit") ?: "unlimited"
            maxLogsLimit.value = maxLimit

            val themeConfig = repository.getConfig("theme_mode") ?: "system"
            isDarkMode.value = when (themeConfig) {
                "dark" -> true
                "light" -> false
                else -> null
            }
        }
    }

    fun updateMaxLogsLimit(limit: String) = viewModelScope.launch {
        maxLogsLimit.value = limit
        repository.setConfig("max_logs_limit", limit)
        repository.pruneLogs()
    }

    fun toggleFloatingBottomBar(enabled: Boolean) = viewModelScope.launch {
        isFloatingBottomBar.value = enabled
        repository.setConfig("floating_bottom_bar", enabled.toString())
    }

    // --- State Stream of all Check-Ins ---
    val checkInsState: StateFlow<List<CheckIn>> = repository.allCheckIns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State Stream of all Operation Logs ---
    val logsState: StateFlow<List<LogEntry>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Statistics States (Computed dynamically from checking list and month) ---
    val statsState: StateFlow<CheckInStats> = combine(checkInsState, calendarYear, calendarMonth) { list, year, month ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // Month string format "yyyy-MM" (e.g., "2026-05")
        val currentMonthStr = String.format(Locale.US, "%04d-%02d", year, month + 1)
        
        // 1. 本月打卡天数
        val checkInsThisMonth = list.filter { it.date.startsWith(currentMonthStr) }.size

        // 2. 共打卡次数
        val totalCheckIns = list.size

        // 3. 最高连续天数 (Streak)
        val epochDays = list.mapNotNull {
            try {
                sdf.parse(it.date)?.time?.div(24 * 60 * 60 * 1000)
            } catch (e: Exception) {
                null
            }
        }.distinct().sorted()

        var maxStreak = 0
        if (epochDays.isNotEmpty()) {
            maxStreak = 1
            var currentStreak = 1
            for (i in 1 until epochDays.size) {
                if (epochDays[i] == epochDays[i - 1] + 1) {
                    currentStreak++
                } else if (epochDays[i] > epochDays[i - 1] + 1) {
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak
                    }
                    currentStreak = 1
                }
            }
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
        }

        CheckInStats(
            thisMonthCount = checkInsThisMonth,
            totalCount = totalCheckIns,
            maxStreakDays = maxStreak
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CheckInStats(0, 0, 0))

    // --- Actions ---

    // Regular or Retroactive Check In
    fun checkInDate(date: String, isMakeUp: Boolean, note: String = "") = viewModelScope.launch {
        val success = repository.getAndCheckIn(date, isMakeUp, note)
        if (success) {
            _confettiTrigger.tryEmit(Unit)
            val msg = if (isMakeUp) "补签成功 [日期: $date]" else "打卡成功 [日期: $date]"
            snackbarMessage.value = msg
        } else {
            snackbarMessage.value = "该日期已存在打卡记录"
        }
    }

    // Delete Check In
    fun deleteCheckInDate(date: String) = viewModelScope.launch {
        val success = repository.deleteCheckIn(date)
        if (success) {
            snackbarMessage.value = "已成功删除打卡记录 [日期: $date]"
        } else {
            snackbarMessage.value = "未找到打卡记录"
        }
    }

    // --- Local Backup Operations ---
    fun exportBackup() = viewModelScope.launch {
        val file = repository.exportToFile()
        if (file != null) {
            snackbarMessage.value = "备份成功! 文件已保存至:\n${file.absolutePath}"
        } else {
            snackbarMessage.value = "备份导出失败，请检查存储权限"
        }
    }

    fun importBackup(context: Context, uri: Uri) = viewModelScope.launch {
        val success = repository.importFromUri(uri)
        if (success) {
            _confettiTrigger.tryEmit(Unit)
            snackbarMessage.value = "备份数据导入合并成功!"
        } else {
            snackbarMessage.value = "数据格式错误或导入失败"
        }
    }

    private var saveUrlJob: Job? = null
    private var saveUserJob: Job? = null
    private var savePassJob: Job? = null
    private var saveFileNameJob: Job? = null

    private suspend fun flushConfig() {
        saveUrlJob?.cancel()
        saveUserJob?.cancel()
        savePassJob?.cancel()
        saveFileNameJob?.cancel()
        repository.setConfig(CheckInRepository.KEY_WEBDAV_URL, webdavUrl.value)
        repository.setConfig(CheckInRepository.KEY_WEBDAV_USER, webdavUser.value)
        repository.setConfig(CheckInRepository.KEY_WEBDAV_PASSWORD, webdavPassword.value)
        repository.setConfig(CheckInRepository.KEY_WEBDAV_REMOTE_FILE, webdavFileName.value)
    }

    fun updateWebDAVUrl(value: String) {
        webdavUrl.value = value
        saveUrlJob?.cancel()
        saveUrlJob = viewModelScope.launch {
            delay(500)
            repository.setConfig(CheckInRepository.KEY_WEBDAV_URL, value)
        }
    }

    fun updateWebDAVUser(value: String) {
        webdavUser.value = value
        saveUserJob?.cancel()
        saveUserJob = viewModelScope.launch {
            delay(500)
            repository.setConfig(CheckInRepository.KEY_WEBDAV_USER, value)
        }
    }

    fun updateWebDAVPassword(value: String) {
        webdavPassword.value = value
        savePassJob?.cancel()
        savePassJob = viewModelScope.launch {
            delay(500)
            repository.setConfig(CheckInRepository.KEY_WEBDAV_PASSWORD, value)
        }
    }

    fun updateWebDAVFileName(value: String) {
        webdavFileName.value = value
        saveFileNameJob?.cancel()
        saveFileNameJob = viewModelScope.launch {
            delay(500)
            repository.setConfig(CheckInRepository.KEY_WEBDAV_REMOTE_FILE, value)
        }
    }

    fun testWebDAVConnection() = viewModelScope.launch {
        webdavSyncStatus.value = WebDavStatus.Loading("正在测试连接...")
        flushConfig()
        val res = repository.testWebDAVConnection(webdavUrl.value, webdavUser.value, webdavPassword.value)
        when (res) {
            is WebDavResult.Success -> {
                webdavSyncStatus.value = WebDavStatus.Success(res.message)
                snackbarMessage.value = res.message
            }
            is WebDavResult.Error -> {
                webdavSyncStatus.value = WebDavStatus.Error(res.message)
                snackbarMessage.value = res.message
            }
        }
    }

    fun syncBackupToWebDAV() = viewModelScope.launch {
        webdavSyncStatus.value = WebDavStatus.Loading("正在上传备份...")
        flushConfig()
        val res = repository.uploadToWebDAV()
        when (res) {
            is WebDavResult.Success -> {
                webdavSyncStatus.value = WebDavStatus.Success(res.message)
                snackbarMessage.value = res.message
            }
            is WebDavResult.Error -> {
                webdavSyncStatus.value = WebDavStatus.Error(res.message)
                snackbarMessage.value = res.message
            }
        }
    }

    fun syncRestoreFromWebDAV() = viewModelScope.launch {
        webdavSyncStatus.value = WebDavStatus.Loading("正在自云端同步...")
        flushConfig()
        val res = repository.downloadFromWebDAV()
        when (res) {
            is WebDavResult.Success -> {
                webdavSyncStatus.value = WebDavStatus.Success(res.message)
                _confettiTrigger.tryEmit(Unit)
                snackbarMessage.value = res.message
            }
            is WebDavResult.Error -> {
                webdavSyncStatus.value = WebDavStatus.Error(res.message)
                snackbarMessage.value = res.message
            }
        }
    }

    fun clearAllData() = viewModelScope.launch {
        repository.clearAllData()
        snackbarMessage.value = "所有打卡数据及日志已被清空"
    }

    fun changeTheme(dark: Boolean?) = viewModelScope.launch {
        isDarkMode.value = dark
        val modeStr = when (dark) {
            true -> "dark"
            false -> "light"
            else -> "system"
        }
        repository.setConfig("theme_mode", modeStr)
    }

    fun navigateMonth(delta: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendarYear.value)
            set(Calendar.MONTH, calendarMonth.value)
            add(Calendar.MONTH, delta)
        }
        calendarYear.value = calendar.get(Calendar.YEAR)
        calendarMonth.value = calendar.get(Calendar.MONTH)
    }

    fun setYearMonth(year: Int, month: Int) {
        calendarYear.value = year
        calendarMonth.value = month
    }
}

data class CheckInStats(
    val thisMonthCount: Int,
    val totalCount: Int,
    val maxStreakDays: Int
)

sealed class WebDavStatus {
    object Idle : WebDavStatus()
    data class Loading(val msg: String) : WebDavStatus()
    data class Success(val msg: String) : WebDavStatus()
    data class Error(val msg: String) : WebDavStatus()
}

class MainViewModelFactory(private val repository: CheckInRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
