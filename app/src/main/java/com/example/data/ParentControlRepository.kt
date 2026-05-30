package com.example.data

import kotlinx.coroutines.flow.Flow

class ParentControlRepository(private val dao: ParentControlDao) {

    // --- Config ---
    val parentConfigFlow: Flow<ParentConfig?> = dao.getParentConfigFlow()

    suspend fun getParentConfigDirect(): ParentConfig? = dao.getParentConfigDirect()

    suspend fun updateParentConfig(config: ParentConfig) {
        dao.saveParentConfig(config)
    }

    // --- Requests ---
    val allRequestsFlow: Flow<List<PermissionRequest>> = dao.getAllRequestsFlow()

    suspend fun insertRequest(request: PermissionRequest) {
        dao.insertRequest(request)
    }

    suspend fun updateRequestStatus(id: Int, status: String) {
        dao.updateRequestStatus(id, status)
    }

    suspend fun deleteRequest(id: Int) {
        dao.deleteRequest(id)
    }

    suspend fun clearAllRequests() {
        dao.clearAllRequests()
    }

    // --- Activity Logs ---
    val allActivityLogsFlow: Flow<List<ActivityLog>> = dao.getAllActivityLogs()

    suspend fun insertActivityLog(appName: String, description: String, level: String) {
        val log = ActivityLog(
            appName = appName,
            description = description,
            statusLevel = level
        )
        dao.insertActivityLog(log)
    }

    suspend fun clearActivityLogs() {
        dao.clearActivityLogs()
    }

    // --- Location Logs ---
    val allLocationLogsFlow: Flow<List<LocationLog>> = dao.getAllLocationLogs()
    val latestLocationFlow: Flow<LocationLog?> = dao.getLatestLocation()

    suspend fun insertLocationLog(placeName: String, latitude: Double, longitude: Double, accuracy: String) {
        val log = LocationLog(
            placeName = placeName,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy
        )
        dao.insertLocationLog(log)
    }

    suspend fun clearLocationLogs() {
        dao.clearLocationLogs()
    }
}
