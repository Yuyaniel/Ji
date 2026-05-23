package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins ORDER BY date DESC")
    fun getAllCheckIns(): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins")
    suspend fun getAllCheckInsSync(): List<CheckIn>

    @Query("SELECT * FROM check_ins WHERE date = :date LIMIT 1")
    suspend fun getCheckInByDate(date: String): CheckIn?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: CheckIn): Long

    @Query("DELETE FROM check_ins WHERE date = :date")
    suspend fun deleteCheckInByDate(date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCheckIns(checkIns: List<CheckIn>)

    @Query("DELETE FROM check_ins")
    suspend fun clearAllCheckIns()
}

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries")
    suspend fun getAllLogsSync(): List<LogEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntry): Long

    @Query("DELETE FROM log_entries WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM log_entries")
    suspend fun clearLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<LogEntry>)
}

@Dao
interface ConfigEntryDao {
    @Query("SELECT value FROM config_entries WHERE `key` = :key LIMIT 1")
    suspend fun getConfig(key: String): String?

    @Query("INSERT OR REPLACE INTO config_entries (`key`, value) VALUES (:key, :value)")
    suspend fun setConfig(key: String, value: String)

    @Query("DELETE FROM config_entries WHERE `key` = :key")
    suspend fun deleteConfig(key: String)
}

@Database(entities = [CheckIn::class, LogEntry::class, ConfigEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun configEntryDao(): ConfigEntryDao
}
