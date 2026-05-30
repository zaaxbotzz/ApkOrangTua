package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentControlDao {

    // --- Parent Config Queries ---
    @Query("SELECT * FROM parent_config WHERE id = 1 LIMIT 1")
    fun getParentConfigFlow(): Flow<ParentConfig?>

    @Query("SELECT * FROM parent_config WHERE id = 1 LIMIT 1")
    suspend fun getParentConfigDirect(): ParentConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveParentConfig(config: ParentConfig)

    // --- Permission Requests Queries ---
    @Query("SELECT * FROM permission_request ORDER BY timestamp DESC")
    fun getAllRequestsFlow(): Flow<List<PermissionRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: PermissionRequest)

    @Query("UPDATE permission_request SET status = :status WHERE id = :id")
    suspend fun updateRequestStatus(id: Int, status: String)

    @Query("DELETE FROM permission_request WHERE id = :id")
    suspend fun deleteRequest(id: Int)

    @Query("DELETE FROM permission_request")
    suspend fun clearAllRequests()

    // --- Activity Log Queries ---
    @Query("SELECT * FROM activity_log ORDER BY timestamp DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("DELETE FROM activity_log")
    suspend fun clearActivityLogs()

    // --- Location Log Queries ---
    @Query("SELECT * FROM location_log ORDER BY timestamp DESC")
    fun getAllLocationLogs(): Flow<List<LocationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationLog(log: LocationLog)

    @Query("SELECT * FROM location_log ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLocation(): Flow<LocationLog?>

    @Query("DELETE FROM location_log")
    suspend fun clearLocationLogs()
}
