package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ParentControlViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ParentControlRepository(db.parentControlDao())

    // Profile state: "SELECT" (chooser), "CHILD", "PARENT"
    private val _currentProfile = MutableStateFlow("SELECT")
    val currentProfile: StateFlow<String> = _currentProfile.asStateFlow()

    // Config screen states
    val parentConfig: StateFlow<ParentConfig?> = repository.parentConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Permission requests
    val allRequests: StateFlow<List<PermissionRequest>> = repository.allRequestsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monitoring logs
    val activityLogs: StateFlow<List<ActivityLog>> = repository.allActivityLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Location logs
    val locationLogs: StateFlow<List<LocationLog>> = repository.allLocationLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestLocation: StateFlow<LocationLog?> = repository.latestLocationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current hour simulation (allows simulating bedtime without waiting for physical 9 PM)
    private val _simulatedHour = MutableStateFlow(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    val simulatedHour: StateFlow<Int> = _simulatedHour.asStateFlow()

    init {
        // Simple background timer to tick remaining emergency overrides
        viewModelScope.launch {
            while (true) {
                delay(10000) // check every 10 seconds
                checkEmergencyExpiry()
            }
        }
    }

    fun setProfile(profile: String) {
        _currentProfile.value = profile
    }

    fun updateSimulatedHour(hour: Int) {
        _simulatedHour.value = hour
    }

    // --- PARENTAL CONFIG ACTIONS ---

    fun verifyPasscode(enteredPin: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val config = repository.getParentConfigDirect() ?: ParentConfig()
            if (config.passcode == enteredPin) {
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun updatePasscode(newPin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = repository.getParentConfigDirect() ?: ParentConfig()
            repository.updateParentConfig(config.copy(passcode = newPin))
            repository.insertActivityLog(
                appName = "Sistem Keamanan",
                description = "PIN Orang Tua berhasil diubah guna meningkatkan keamanan.",
                level = "NORMAL"
            )
        }
    }

    fun toggleManualLock() {
        viewModelScope.launch(Dispatchers.IO) {
            val config = repository.getParentConfigDirect() ?: ParentConfig()
            val newStage = !config.isManuallyLocked
            repository.updateParentConfig(config.copy(isManuallyLocked = newStage))
            
            repository.insertActivityLog(
                appName = "Kontrol Manual",
                description = if (newStage) "HP Anak dikunci manual oleh Orang Tua." else "Kunci manual HP Anak dibuka oleh Orang Tua.",
                level = if (newStage) "WARN" else "NORMAL"
            )
        }
    }

    fun updateDailyLimit(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = repository.getParentConfigDirect() ?: ParentConfig()
            repository.updateParentConfig(config.copy(dailyLimitMinutes = minutes))
            repository.insertActivityLog(
                appName = "Batasan Waktu",
                description = "Batas layar harian anak diubah menjadi $minutes menit.",
                level = "NORMAL"
            )
        }
    }

    fun updateBedtimeSchedule(startHour: Int, endHour: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = repository.getParentConfigDirect() ?: ParentConfig()
            repository.updateParentConfig(config.copy(bedtimeStartHour = startHour, bedtimeEndHour = endHour))
            repository.insertActivityLog(
                appName = "Jam Malam",
                description = "Jadwal istirahat anak diubah: Mulai pukul $startHour.00 s/d $endHour.00.",
                level = "NORMAL"
            )
        }
    }

    fun simulateTimeSpent(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = repository.getParentConfigDirect() ?: ParentConfig()
            val currentUsed = config.minutesUsedToday
            val newUsed = (currentUsed + minutes).coerceAtLeast(0)
            repository.updateParentConfig(config.copy(minutesUsedToday = newUsed))

            if (newUsed >= config.dailyLimitMinutes) {
                repository.insertActivityLog(
                    appName = "Sistem Pengunci",
                    description = "HP Anak terdeteksi mengunci otomatis karena penggunaan layar hari ini sudah mencapai batas harian ($newUsed/${config.dailyLimitMinutes} mnt).",
                    level = "WARN"
                )
            }
        }
    }

    fun resetDailyTimeSpent() {
        viewModelScope.launch(Dispatchers.IO) {
            val config = repository.getParentConfigDirect() ?: ParentConfig()
            repository.updateParentConfig(config.copy(minutesUsedToday = 0))
            repository.insertActivityLog(
                appName = "Batas Layar",
                description = "Penggunaan waktu screen-time anak di-reset ke 0 menit hari ini oleh Orang Tua.",
                level = "NORMAL"
            )
        }
    }

    private suspend fun checkEmergencyExpiry() {
        val config = repository.getParentConfigDirect() ?: return
        if (config.isEmergencyUnlockActive && config.emergencyUnlockExpiry < System.currentTimeMillis()) {
            repository.updateParentConfig(
                config.copy(
                    isEmergencyUnlockActive = false,
                    emergencyUnlockExpiry = 0L
                )
            )
            repository.insertActivityLog(
                appName = "Timeout Darurat",
                description = "Waktu tambahan izin darurat telah selesai. Pembatasan layar diaktifkan kembali.",
                level = "WARN"
            )
        }
    }

    // --- PERMISSION REQUEST ACTIONS (DIKONTROL ANAK / ORANG TUA) ---

    fun submitPermissionRequest(title: String, reason: String, requestedMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val request = PermissionRequest(
                title = title,
                reason = reason,
                requestedMinutes = requestedMinutes,
                status = "PENDING"
            )
            repository.insertRequest(request)
            repository.insertActivityLog(
                appName = "Permohonan Izin",
                description = "Anak mengirimkan permintaan tambahan waktu layar selama $requestedMinutes mnt dengan alasan: '$reason'.",
                level = "NORMAL"
            )
        }
    }

    fun approveRequest(id: Int, requestedMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRequestStatus(id, "APPROVED")
            val config = repository.getParentConfigDirect() ?: ParentConfig()
            
            // Grant time by activating emergency unlock for the requested time
            val additionalMs = requestedMinutes * 60 * 1000L
            val currentExpiry = if (config.isEmergencyUnlockActive) config.emergencyUnlockExpiry else System.currentTimeMillis()
            
            repository.updateParentConfig(
                config.copy(
                    isEmergencyUnlockActive = true,
                    emergencyUnlockExpiry = currentExpiry + additionalMs
                )
            )

            repository.insertActivityLog(
                appName = "Persetujuan Izin",
                description = "Orang Tua MENYETUJUI permohonan tambahan waktu $requestedMinutes menit.",
                level = "NORMAL"
            )
        }
    }

    fun rejectRequest(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRequestStatus(id, "REJECTED")
            repository.insertActivityLog(
                appName = "Sistem Pengawas",
                description = "Orang Tua MENOLAK permohonan tambahan waktu anak.",
                level = "WARN"
            )
        }
    }

    fun clearAllRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllRequests()
        }
    }

    // --- ACTIVITY MONITORING ACTIONS ---

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearActivityLogs()
        }
    }

    fun addCustomMockLog(app: String, desc: String, level: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertActivityLog(app, desc, level)
        }
    }

    // --- LOCATION TRACKING ACTIONS ---

    fun clearLocations() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLocationLogs()
        }
    }

    fun addCustomLocation(placeName: String, lat: Double, lng: Double, accuracy: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLocationLog(placeName, lat, lng, accuracy)
            repository.insertActivityLog(
                appName = "Lokasi GPS",
                description = "Anak terpantau berpindah lokasi ke $placeName.",
                level = "NORMAL"
            )
        }
    }

    // Derived locking check
    fun isCurrentlyLocked(config: ParentConfig, currentHour: Int): Boolean {
        if (config.isManuallyLocked) return true

        // Checking emergency / approved override status
        val isEmergencyActive = config.isEmergencyUnlockActive && config.emergencyUnlockExpiry > System.currentTimeMillis()
        if (isEmergencyActive) return false

        // Daily limit exceeded Check
        if (config.minutesUsedToday >= config.dailyLimitMinutes) return true

        // Bedtime schedule Check
        val start = config.bedtimeStartHour
        val end = config.bedtimeEndHour
        if (start != end) {
            if (start > end) {
                // Overnight bedtime rule e.g. 21.00 s/d 06.00
                if (currentHour >= start || currentHour < end) return true
            } else {
                // Inside range rule
                if (currentHour in start until end) return true
            }
        }
        return false
    }
}
