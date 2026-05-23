package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class CheckInRepository(
    private val context: Context,
    private val checkInDao: CheckInDao,
    private val logEntryDao: LogEntryDao,
    private val configEntryDao: ConfigEntryDao
) {
    val allCheckIns: Flow<List<CheckIn>> = checkInDao.getAllCheckIns()
    val allLogs: Flow<List<LogEntry>> = logEntryDao.getAllLogs()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Configuration keys
    companion object {
        const val KEY_WEBDAV_URL = "webdav_url"
        const val KEY_WEBDAV_USER = "webdav_user"
        const val KEY_WEBDAV_PASSWORD = "webdav_password"
        const val KEY_WEBDAV_REMOTE_FILE = "webdav_file_name" // default to "star_habits_backup.json"
    }

    suspend fun getAndCheckIn(date: String, isMakeUp: Boolean, note: String = ""): Boolean = withContext(Dispatchers.IO) {
        val existing = checkInDao.getCheckInByDate(date)
        if (existing == null) {
            val type = if (isMakeUp) "MAKE_UP" else "CHECK_IN"
            val checkIn = CheckIn(date, type, System.currentTimeMillis(), note = note)
            checkInDao.insertCheckIn(checkIn)

            val actionType = if (isMakeUp) "MAKE_UP" else "CHECK_IN"
            val noteSuffix = if (note.isNotBlank()) " [备注: $note]" else ""
            val logMsg = if (isMakeUp) "补签成功 [日期: $date]$noteSuffix" else "打卡成功 [日期: $date]$noteSuffix"
            val log = LogEntry(
                date = date,
                action = actionType,
                timestamp = System.currentTimeMillis(),
                details = logMsg
            )
            insertLogAndPrune(log)
            return@withContext true
        }
        return@withContext false
    }

    suspend fun insertLogAndPrune(log: LogEntry) {
        logEntryDao.insertLog(log)
        pruneLogs()
    }

    suspend fun pruneLogs() = withContext(Dispatchers.IO) {
        val maxLimitStr = configEntryDao.getConfig("max_logs_limit") ?: "unlimited"
        val limit = maxLimitStr.toIntOrNull()
        if (limit != null && limit > 0) {
            val allLogs = logEntryDao.getAllLogsSync()
            if (allLogs.size > limit) {
                val logsToDelete = allLogs.sortedBy { it.timestamp }.take(allLogs.size - limit)
                for (oldLog in logsToDelete) {
                    logEntryDao.deleteLogById(oldLog.id)
                }
            }
        }
    }

    suspend fun deleteCheckIn(date: String): Boolean = withContext(Dispatchers.IO) {
        val existing = checkInDao.getCheckInByDate(date)
        if (existing != null) {
            checkInDao.deleteCheckInByDate(date)
            val log = LogEntry(
                date = date,
                action = "DELETE",
                timestamp = System.currentTimeMillis(),
                details = "删除打卡记录 [日期: $date]"
            )
            insertLogAndPrune(log)
            return@withContext true
        }
        return@withContext false
    }

    // --- Configurations ---
    suspend fun setConfig(key: String, value: String) {
        configEntryDao.setConfig(key, value)
    }

    suspend fun getConfig(key: String): String? {
        return configEntryDao.getConfig(key)
    }

    // --- JSON Backup Generator & Parser ---
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val checkIns = checkInDao.getAllCheckInsSync()
        val logs = logEntryDao.getAllLogsSync()

        val rootObj = JSONObject()
        rootObj.put("version", 1)

        val checkInArr = JSONArray()
        for (item in checkIns) {
            val obj = JSONObject()
            obj.put("date", item.date)
            obj.put("type", item.type)
            obj.put("timestamp", item.timestamp)
            obj.put("note", item.note)
            checkInArr.put(obj)
        }
        rootObj.put("checkIns", checkInArr)

        val logArr = JSONArray()
        for (item in logs) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("date", item.date)
            obj.put("action", item.action)
            obj.put("timestamp", item.timestamp)
            obj.put("details", item.details)
            logArr.put(obj)
        }
        rootObj.put("logs", logArr)

        return@withContext rootObj.toString(2)
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootObj = JSONObject(jsonStr)
            val checkInArr = rootObj.optJSONArray("checkIns")
            if (checkInArr != null) {
                val checkInList = mutableListOf<CheckIn>()
                for (i in 0 until checkInArr.length()) {
                    val obj = checkInArr.getJSONObject(i)
                    checkInList.add(
                        CheckIn(
                            date = obj.getString("date"),
                            type = obj.optString("type", "CHECK_IN"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            note = obj.optString("note", "")
                        )
                    )
                }
                checkInDao.insertAllCheckIns(checkInList)
            }

            val logArr = rootObj.optJSONArray("logs")
            if (logArr != null) {
                val logList = mutableListOf<LogEntry>()
                for (i in 0 until logArr.length()) {
                    val obj = logArr.getJSONObject(i)
                    logList.add(
                        LogEntry(
                            date = obj.getString("date"),
                            action = obj.optString("action", "CHECK_IN"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            details = obj.optString("details", "")
                        )
                    )
                }
                logEntryDao.insertAllLogs(logList)
                pruneLogs()
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    // --- Local Backup Export (Write to file in External cache or downloads) ---
    suspend fun exportToFile(): File? = withContext(Dispatchers.IO) {
        try {
            val contents = exportDataToJson()
            val dir = context.getExternalFilesDir(null) ?: context.cacheDir
            val file = File(dir, "star_habits_backup_${System.currentTimeMillis() / 1000}.json")
            FileOutputStream(file).use { output ->
                output.write(contents.toByteArray())
            }
            return@withContext file
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun importFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readBytes()
                val jsonStr = String(bytes)
                return@withContext importDataFromJson(jsonStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    // --- WebDAV Synchronizations ---
    suspend fun testWebDAVConnection(url: String, user: String, pass: String): WebDavResult = withContext(Dispatchers.IO) {
        if (url.isBlank() || user.isBlank() || pass.isBlank()) {
            return@withContext WebDavResult.Error("参数不可为空")
        }
        try {
            val credential = Credentials.basic(user, pass)
            
            // Format URL: ensure it starts with http:// or https://
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            // Ensure trailing slash
            if (!formattedUrl.endsWith("/")) {
                formattedUrl = "$formattedUrl/"
            }

            // We do a simple request to check if the WebDAV endpoint is reachable and authenticates
            val request = Request.Builder()
                .url(formattedUrl)
                .header("Authorization", credential)
                .method("PROPFIND", "".toRequestBody("text/xml".toMediaType()))
                .header("Depth", "0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 207 || response.code == 200) {
                    return@withContext WebDavResult.Success("连接测试成功")
                } else if (response.code == 401) {
                    return@withContext WebDavResult.Error("用户名或密码错误 (401)")
                } else {
                    // Try simple GET if PROPFIND is filtered/blocked
                    val getRequest = Request.Builder()
                        .url(formattedUrl)
                        .header("Authorization", credential)
                        .get()
                        .build()
                    okHttpClient.newCall(getRequest).execute().use { r2 ->
                        if (r2.isSuccessful || r2.code == 200) {
                            return@withContext WebDavResult.Success("连接测试成功 (GET)")
                        }
                        return@withContext WebDavResult.Error("连接失败，状态码: ${r2.code}")
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext WebDavResult.Error("连接失败: ${e.message ?: e.toString()}")
        }
    }

    suspend fun uploadToWebDAV(): WebDavResult = withContext(Dispatchers.IO) {
        val url = configEntryDao.getConfig(KEY_WEBDAV_URL) ?: ""
        val user = configEntryDao.getConfig(KEY_WEBDAV_USER) ?: ""
        val pass = configEntryDao.getConfig(KEY_WEBDAV_PASSWORD) ?: ""
        val fileName = configEntryDao.getConfig(KEY_WEBDAV_REMOTE_FILE) ?: "star_habits_backup.json"

        if (url.isBlank() || user.isBlank() || pass.isBlank()) {
            return@withContext WebDavResult.Error("WebDAV 未配置或配置不全")
        }

        try {
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            if (!formattedUrl.endsWith("/")) {
                formattedUrl = "$formattedUrl/"
            }
            val uploadUrl = "$formattedUrl$fileName"
            val credential = Credentials.basic(user, pass)

            val localJson = exportDataToJson()
            val requestBody = localJson.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", credential)
                .put(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200 || response.code == 201 || response.code == 204) {
                    return@withContext WebDavResult.Success("备份成功，已上传至 WebDAV")
                } else {
                    return@withContext WebDavResult.Error("上传失败，服务返回状态码: ${response.code}")
                }
            }
        } catch (e: Exception) {
            return@withContext WebDavResult.Error("上传中断: ${e.message ?: e.toString()}")
        }
    }

    suspend fun downloadFromWebDAV(): WebDavResult = withContext(Dispatchers.IO) {
        val url = configEntryDao.getConfig(KEY_WEBDAV_URL) ?: ""
        val user = configEntryDao.getConfig(KEY_WEBDAV_USER) ?: ""
        val pass = configEntryDao.getConfig(KEY_WEBDAV_PASSWORD) ?: ""
        val fileName = configEntryDao.getConfig(KEY_WEBDAV_REMOTE_FILE) ?: "star_habits_backup.json"

        if (url.isBlank() || user.isBlank() || pass.isBlank()) {
            return@withContext WebDavResult.Error("WebDAV 未配置或配置不全")
        }

        try {
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            if (!formattedUrl.endsWith("/")) {
                formattedUrl = "$formattedUrl/"
            }
            val downloadUrl = "$formattedUrl$fileName"
            val credential = Credentials.basic(user, pass)

            val request = Request.Builder()
                .url(downloadUrl)
                .header("Authorization", credential)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext WebDavResult.Error("WebDAV 服务器上未发现云备份文件 [$fileName]")
                }
                if (!response.isSuccessful) {
                    return@withContext WebDavResult.Error("拉取失败，服务状态码: ${response.code}")
                }
                val bodyStr = response.body?.string() ?: ""
                if (bodyStr.isBlank()) {
                    return@withContext WebDavResult.Error("云备份文件为空")
                }

                val success = importDataFromJson(bodyStr)
                if (success) {
                    return@withContext WebDavResult.Success("下载备份合并成功!")
                } else {
                    return@withContext WebDavResult.Error("解析云备份文件失败，可能不是合法的 JSON")
                }
            }
        } catch (e: Exception) {
            return@withContext WebDavResult.Error("拉取中断: ${e.message ?: e.toString()}")
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        checkInDao.clearAllCheckIns()
        logEntryDao.clearLogs()
    }
}

sealed class WebDavResult {
    data class Success(val message: String) : WebDavResult()
    data class Error(val message: String) : WebDavResult()
}
