package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ParentConfig::class,
        PermissionRequest::class,
        ActivityLog::class,
        LocationLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun parentControlDao(): ParentControlDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parent_control_db"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.parentControlDao())
                }
            }
        }

        suspend fun populateDatabase(dao: ParentControlDao) {
            // 1. Initial Parent Configuration
            val defaultConfig = ParentConfig(
                id = 1,
                passcode = "1234",
                isManuallyLocked = false,
                dailyLimitMinutes = 120,
                minutesUsedToday = 45,
                bedtimeStartHour = 21, // 9 PM
                bedtimeEndHour = 6,    // 6 AM
                childDeviceName = "HP Budi (Samsung Galaxy A35)",
                isEmergencyUnlockActive = false,
                emergencyUnlockExpiry = 0L
            )
            dao.saveParentConfig(defaultConfig)

            // 2. Initial Sample/Prepopulated Activity Logs for Monitoring
            val logs = listOf(
                ActivityLog(
                    appName = "Duolingo",
                    description = "Membuka aplikasi pembelajaran bahasa asing. Durasi: 25 menit.",
                    statusLevel = "NORMAL",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 15 // 15 mins ago
                ),
                ActivityLog(
                    appName = "Pencarian Google Chrome",
                    description = "Mencari kata kunci: 'rumus fisika gaya gravitasi kls 8'",
                    statusLevel = "NORMAL",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 45 // 45 mins ago
                ),
                ActivityLog(
                    appName = "Mobile Legends",
                    description = "Membuka game online di jam belajar sore. Pengawasan otomatis mendeteksi aktivitas non-edukasi.",
                    statusLevel = "WARN",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 120 // 2 hours ago
                ),
                ActivityLog(
                    appName = "TikTok",
                    description = "Membuka aplikasi media sosial tanpa izin di luar jadwal senggang.",
                    statusLevel = "WARN",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 300 // 5 hours ago
                ),
                ActivityLog(
                    appName = "Google Chrome",
                    description = "Terblokir otomatis: Mencoba mengakses kata kunci mencurigakan 'cheat game diamonds gratis no root'.",
                    statusLevel = "DANGER",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 480 // 8 hours ago
                )
            )

            for (log in logs) {
                dao.insertActivityLog(log)
            }

            // 3. Initial Sample Location Logs (Simulated tracking)
            val locations = listOf(
                LocationLog(
                    placeName = "Bimbel Harapan Sukses",
                    latitude = -6.1824,
                    longitude = 106.8294,
                    accuracy = "Akurasi Tinggi (GPS - 5m)",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 30 // 30 mins ago
                ),
                LocationLog(
                    placeName = "Sekolah Menengah Pertama 12",
                    latitude = -6.1895,
                    longitude = 106.8210,
                    accuracy = "Akurasi Sedang (Wi-Fi - 15m)",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 240 // 4 hours ago
                ),
                LocationLog(
                    placeName = "Area Rumah Tinggal",
                    latitude = -6.1950,
                    longitude = 106.8025,
                    accuracy = "Akurasi Tinggi (GPS - 3m)",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 600 // 10 hours ago
                )
            )

            for (loc in locations) {
                dao.insertLocationLog(loc)
            }

            // 4. Initial Sample Request
            val request = PermissionRequest(
                title = "Minta tambahan waktu belajar",
                reason = "Ingin menyelesaikan kuis bahasa Inggris di Duolingo karena ada streak harian.",
                requestedMinutes = 30,
                status = "PENDING",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 10 // 10 mins ago
            )
            dao.insertRequest(request)
        }
    }
}
