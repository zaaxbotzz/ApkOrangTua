package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parent_config")
data class ParentConfig(
    @PrimaryKey val id: Int = 1, // Single-row config
    val passcode: String = "1234",
    val isManuallyLocked: Boolean = false,
    val dailyLimitMinutes: Int = 120,
    val minutesUsedToday: Int = 45,
    val bedtimeStartHour: Int = 21, // 9 PM
    val bedtimeEndHour: Int = 6,    // 6 AM
    val childDeviceName: String = "HP Anak (Samsung A54)",
    val isEmergencyUnlockActive: Boolean = false,
    val emergencyUnlockExpiry: Long = 0L
)

@Entity(tableName = "permission_request")
data class PermissionRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val reason: String,
    val requestedMinutes: Int,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_log")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val description: String,
    val statusLevel: String = "NORMAL", // NORMAL, WARN, DANGER
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "location_log")
data class LocationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: String,
    val timestamp: Long = System.currentTimeMillis()
)
