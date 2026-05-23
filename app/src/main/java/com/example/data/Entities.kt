package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_ins")
data class CheckIn(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val type: String,            // "CHECK_IN" (normal check-in) or "MAKE_UP" (retroactive)
    val timestamp: Long,         // System.currentTimeMillis()
    val note: String = ""        // optional user remarks/notes
)

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // "yyyy-MM-dd" (the check-in date)
    val action: String,          // "CHECK_IN", "MAKE_UP", "DELETE"
    val timestamp: Long,         // system time of log action
    val details: String          // human-readable message, e.g., "进行了打卡"
)

@Entity(tableName = "config_entries")
data class ConfigEntry(
    @PrimaryKey val key: String,
    val value: String
)
