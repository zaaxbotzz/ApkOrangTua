package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.MockMap
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ParentControlViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize the central Parental Control ViewModel
                val viewModel: ParentControlViewModel = viewModel()
                MainAppContainer(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppContainer(viewModel: ParentControlViewModel) {
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val config by viewModel.parentConfig.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9FBFC))
        ) {
            AnimatedContent(
                targetState = currentProfile,
                transitionSpec = {
                    fadeIn() + scaleIn(initialScale = 0.95f) togetherWith
                            fadeOut() + scaleOut(targetScale = 0.95f)
                },
                label = "profile_anim"
            ) { profile ->
                when (profile) {
                    "SELECT" -> ProfileSelectScreen(
                        onSelectChild = { viewModel.setProfile("CHILD") },
                        onSelectParent = { viewModel.setProfile("PARENT_AUTH") }
                    )
                    "CHILD" -> {
                        config?.let { activeConfig ->
                            ChildScreenWrapper(viewModel, activeConfig)
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    "PARENT_AUTH" -> {
                        ParentAuthPinScreen(
                            viewModel = viewModel,
                            onAuthenticated = { viewModel.setProfile("PARENT") },
                            onBackToSelect = { viewModel.setProfile("SELECT") }
                        )
                    }
                    "PARENT" -> {
                        ParentDashboardLayout(
                            viewModel = viewModel,
                            onExitDashboard = { viewModel.setProfile("SELECT") }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. PROFILE SELECTION / ONBOARDING SCREEN
// ==========================================
@Composable
fun ProfileSelectScreen(
    onSelectChild: () -> Unit,
    onSelectParent: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFDF7FF), Color(0xFFF3EDF7), Color(0xFFEADDFF).copy(alpha = 0.5f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Logo / Anchor
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A8A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Kunci Keluarga",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Solusi Pengawasan & Kunci Layar Anak Pintar",
                fontSize = 15.sp,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )

            Text(
                text = "PILIH PERAN PERANGKAT:",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Child profile card button
            ProfileButtonCard(
                title = "📱 Perangkat Anak",
                description = "Tampilan hp anak yang terpantau secara berkala & terikat aturan screen-time.",
                primaryColor = Color(0xFF3B82F6),
                icon = Icons.Default.Person,
                onClick = onSelectChild
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Parent profile card button
            ProfileButtonCard(
                title = "🛡️ Dashboard Orang Tua",
                description = "Pantau statistik harian anak, berikan ijin permohonan, & konfigurasikan proteksi.",
                primaryColor = Color(0xFF0F172A),
                icon = Icons.Default.Lock,
                onClick = onSelectParent
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Subtitle instructions
            Text(
                text = "Default PIN Orang Tua: 1234",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ProfileButtonCard(
    title: String,
    description: String,
    primaryColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(primaryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ==========================================
// 2. PARENT PIN AUTHENTICATION SCREEN
// ==========================================
@Composable
fun ParentAuthPinScreen(
    viewModel: ParentControlViewModel,
    onAuthenticated: () -> Unit,
    onBackToSelect: () -> Unit
) {
    val context = LocalContext.current
    var pinValue by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFDF7FF), Color(0xFFF3EDF7), Color(0xFFEADDFF).copy(alpha = 0.5f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Button Back
            IconButton(
                onClick = onBackToSelect,
                modifier = Modifier
                    .align(Alignment.Start)
                    .background(Color(0xFFEADDFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kembali",
                    tint = Color(0xFF21005D)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Gembok",
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Proteksi Orang Tua",
                fontSize = 26.sp,
                color = Color(0xFF1D1B20),
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Masukkan kode PIN keamanan untuk masuk ke dashboard pengawasan",
                fontSize = 13.sp,
                color = Color(0xFF49454F),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Simulated Rounded Code Dots Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(4) { index ->
                    val isFilled = index < pinValue.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    hasError -> Color(0xFFB3261E)
                                    isFilled -> Color(0xFF6750A4)
                                    else -> Color(0xFFCAC4D0)
                                }
                            )
                    )
                }
            }

            // Tactile Keypad (0-9, Clear, Delete)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                val padKeys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                for (row in padKeys) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.24f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.55f))
                                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        hasError = false
                                        when (key) {
                                            "C" -> pinValue = ""
                                            "⌫" -> {
                                                if (pinValue.isNotEmpty()) {
                                                    pinValue = pinValue.dropLast(1)
                                                }
                                            }
                                            else -> {
                                                if (pinValue.length < 4) {
                                                    pinValue += key
                                                    // Trigger Verification once 4 numbers are entered
                                                    if (pinValue.length == 4) {
                                                        viewModel.verifyPasscode(
                                                            enteredPin = pinValue,
                                                            onSuccess = {
                                                                Toast.makeText(context, "Verifikasi PIN Sukses!", Toast.LENGTH_SHORT).show()
                                                                onAuthenticated()
                                                            },
                                                            onError = {
                                                                hasError = true
                                                                pinValue = ""
                                                                Toast.makeText(context, "PIN Salah! Gagal masuk.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 20.sp,
                                    color = Color(0xFF1D1B20),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper to remember string state
@Composable
fun rememberMutableStateFlowOf(value: String) = remember { mutableStateOf(value) }

// ===============================================
// 3. CHILD SCREEN WRAPPER (DASHBOARD & OVERLAY LOCK)
// ===============================================
@Composable
fun ChildScreenWrapper(
    viewModel: ParentControlViewModel,
    config: ParentConfig
) {
    val simulatedHour by viewModel.simulatedHour.collectAsStateWithLifecycle()
    val isLocked = viewModel.isCurrentlyLocked(config, simulatedHour)

    Box(modifier = Modifier.fillMaxSize()) {
        // Under: The Child's Smartphone View (if unlocked, or visible beneath shadow)
        ChildDeviceScreen(
            viewModel = viewModel,
            config = config,
            isLocked = isLocked
        )

        // Over: The Child Lockscreen overlay overlaying everything
        if (isLocked) {
            ChildLockScreenOverlay(
                viewModel = viewModel,
                config = config,
                simulatedHour = simulatedHour
            )
        }
    }
}

// Child Dashboard Application Simulated Screen
@Composable
fun ChildDeviceScreen(
    viewModel: ParentControlViewModel,
    config: ParentConfig,
    isLocked: Boolean
) {
    val logs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Child Device Header Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = config.childDeviceName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Profil Anak Terproteksi",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E88E5),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isLocked) Color(0xFFFFECEF) else Color(0xFFEDFBF7),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isLocked) "Terikat Kunci" else "Aktif/Aman",
                            fontSize = 11.sp,
                            color = if (isLocked) Color(0xFFEF4444) else Color(0xFF009688),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Diagnostic Screen-Time Budget Progress Circle / Info
                val progress = (config.minutesUsedToday.toFloat() / config.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
                val isLimitExceeded = config.minutesUsedToday >= config.dailyLimitMinutes

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${config.minutesUsedToday} mnt",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLimitExceeded) Color(0xFFEF4444) else Color(0xFF3B82F6)
                        )
                        Text(
                            text = "Telah Digunakan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    // A nice Circular percentage
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                        CircularProgressIndicator(
                            progress = progress,
                            strokeWidth = 6.dp,
                            color = if (isLimitExceeded) Color(0xFFEF4444) else Color(0xFF10B981),
                            trackColor = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${config.dailyLimitMinutes} mnt",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Batas Harian",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Active Time info
                val activeOverride = config.isEmergencyUnlockActive && config.emergencyUnlockExpiry > System.currentTimeMillis()
                if (activeOverride) {
                    val remMs = config.emergencyUnlockExpiry - System.currentTimeMillis()
                    val remMins = (remMs / (1000 * 60)).coerceAtLeast(0L)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(Color(0xFFFFFAEC), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "🛡️ Izin darurat aktif: Tambahan $remMins menit disetujui orang tua.",
                            fontSize = 12.sp,
                            color = Color(0xFFD97706),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Simulated Apps Showcase (Allows kid to play games/study and simulate usage)
        Text(
            text = "SIMULASI APLIKASI:",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Klik aplikasi di bawah untuk mensimulasikan penggunaan 10 menit. Ini akan menambah waktu pakai dan mengirim log pantauan ke Orang Tua secara real-time.",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Grids of mock apps
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val apps = listOf(
                MockAppItem("Duolingo (Pendidikan)", "Belajar Bahasa Inggris", Color(0xFF22C55E), "https://duolingo.com", "NORMAL"),
                MockAppItem("Roblox (Entertainment)", "Bermain game multiplayer online", Color(0xFFEF4444), "https://roblox.com", "WARN"),
                MockAppItem("Chrome Browser", "Membuka browser pencarian google", Color(0xFFFBBF24), "https://chrome.com", "NORMAL"),
                MockAppItem("Mobile Legends", "Mabar game MOBA 5v5 sore hari", Color(0xFF6366F1), "https://mlas.com", "WARN")
            )

            for (app in apps) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(app.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (app.type == "NORMAL") Icons.Default.Check else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = app.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = app.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = app.desc,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isLocked) {
                                    Toast.makeText(context, "Tidak bisa dibuka! HP sedang dikunci parenting.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.simulateTimeSpent(10)
                                    viewModel.addCustomMockLog(
                                        app = app.name,
                                        desc = "Membuka ${app.name} dan menggunakannya selama 10 menit.",
                                        level = app.type
                                    )
                                    Toast.makeText(context, "Selesai mensimulasikan ${app.name} (10 menit)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLocked) Color(0xFFCBD5E1) else app.color
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Buka App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Device profile back buttons
        Button(
            onClick = { viewModel.setProfile("SELECT") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kembali ke Switch Profil")
        }
    }
}

data class MockAppItem(
    val name: String,
    val desc: String,
    val color: Color,
    val link: String,
    val type: String
)

// ===============================================
// 4. CHILD LOCK WINDOW OVERLAY (KUNCI LAYAR ANAK)
// ===============================================
@Composable
fun ChildLockScreenOverlay(
    viewModel: ParentControlViewModel,
    config: ParentConfig,
    simulatedHour: Int
) {
    val requests by viewModel.allRequests.collectAsStateWithLifecycle()
    var isSendingRequest by remember { mutableStateOf(false) }

    // Child input form values
    var reasonInput by remember { mutableStateOf("") }
    var requestedMinutes by remember { mutableStateOf(30) } // default adds 30 minutes
    var openDirectPinOverride by remember { mutableStateOf(false) }
    var pinOverrideValue by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(enabled = true, onClick = {}), // Block all back clicks
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lock Info Header
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Gembok Terkunci",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Layar Terkunci",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Reason label translation
            val lockReason = when {
                config.isManuallyLocked -> "Parent mengaktifkan kunci manual jarak jauh."
                config.minutesUsedToday >= config.dailyLimitMinutes -> "Batas waktu harian (${config.dailyLimitMinutes} menit) telah terlampaui."
                simulatedHour >= config.bedtimeStartHour || simulatedHour < config.bedtimeEndHour -> "Masuk jam malam istirahat (${config.bedtimeStartHour}.00 - ${config.bedtimeEndHour}.00)."
                else -> "Pembatasan aktif."
            }

            Text(
                text = lockReason,
                fontSize = 13.sp,
                color = Color(0xFFEF4444),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Kamu bisa meminta kunci ini dibuka dengan mengirimkan permohonan waktu tambahan langsung ke HP Orang Tua, atau panggil ayah/ibu untuk memasukkan PIN secara manual.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Show active pending request if exits
            val pendingReq = requests.firstOrNull { it.status == "PENDING" }

            if (pendingReq != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Menunggu Persetujuan Orang Tua...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            text = "Permohonan +${pendingReq.requestedMinutes} mnt: \"${pendingReq.reason}\"",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                if (!isSendingRequest) {
                    Button(
                        onClick = { isSendingRequest = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Minta Tambahan Waktu", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Send request form dialog
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "KIRIM PERMOHONAN:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Choose requested duration
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(15, 30, 60).forEach { mins ->
                                val selected = requestedMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (selected) Color(0xFF3B82F6) else Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { requestedMinutes = mins }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$mins Menit",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Text Field for reason
                        OutlinedTextField(
                            value = reasonInput,
                            onValueChange = { reasonInput = it },
                            placeholder = { Text("Tulis alasan (cth: Belajar PR Matematika...)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { isSendingRequest = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Batal")
                            }

                            Button(
                                onClick = {
                                    if (reasonInput.trim().isEmpty()) {
                                        Toast.makeText(context, "Harap tuliskan alasan permohonan!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.submitPermissionRequest(
                                            title = "Izin tambahan $requestedMinutes menit",
                                            reason = reasonInput,
                                            requestedMinutes = requestedMinutes
                                        )
                                        Toast.makeText(context, "Permohonan terkirim ke Orang Tua!", Toast.LENGTH_SHORT).show()
                                        reasonInput = ""
                                        isSendingRequest = false
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Kirim Izin")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Immediate Parent PIN Override Option
            if (!openDirectPinOverride) {
                TextButton(
                    onClick = { openDirectPinOverride = true }
                ) {
                    Text("Buka dengan PIN Orang Tua di Sini", color = Color(0xFF64748B), textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline, fontSize = 12.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Masukkan PIN Orang Tua:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    OutlinedTextField(
                        value = pinOverrideValue,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinOverrideValue = it
                                if (it.length == 4) {
                                    viewModel.verifyPasscode(
                                        enteredPin = it,
                                        onSuccess = {
                                            viewModel.resetDailyTimeSpent()
                                            viewModel.approveRequest(999, 60) // simulate 1hr approved unlocked
                                            Toast.makeText(context, "PIN Benar! Sukses dibuka selama 1 jam.", Toast.LENGTH_SHORT).show()
                                            pinOverrideValue = ""
                                            openDirectPinOverride = false
                                        },
                                        onError = {
                                            Toast.makeText(context, "PIN salah!", Toast.LENGTH_SHORT).show()
                                            pinOverrideValue = ""
                                        }
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    TextButton(onClick = { openDirectPinOverride = false }) {
                        Text("Tutup", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Switch Profile Button
            OutlinedButton(
                onClick = { viewModel.setProfile("SELECT") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Ganti Profil")
            }
        }
    }
}

// ==========================================
// 5. PARENT DASHBOARD LAYOUT & TABS
// ==========================================
@Composable
fun ParentDashboardLayout(
    viewModel: ParentControlViewModel,
    onExitDashboard: () -> Unit
) {
    var activeTab by remember { mutableStateOf("HOME") }
    val config by viewModel.parentConfig.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Parent Header Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFDF7FF).copy(alpha = 0.85f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kunci Keluarga",
                            color = Color(0xFF1D1B20),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "Monitor: HP Aiden",
                        color = Color(0xFF49454F),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Log Out / Profile switch
                IconButton(
                    onClick = onExitDashboard,
                    modifier = Modifier.background(Color(0xFFEADDFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Ganti Profil",
                        tint = Color(0xFF21005D)
                    )
                }
            }
        }

        // Inner dynamic container body based on tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            config?.let { activeConfig ->
                when (activeTab) {
                    "HOME" -> ParentHomeScreen(viewModel, activeConfig)
                    "REQUEST" -> ParentRequestScreen(viewModel)
                    "MONITOR" -> ParentMonitorScreen(viewModel)
                    "MAP" -> ParentLocationScreen(viewModel)
                    "SETTINGS" -> ParentSettingsScreen(viewModel, activeConfig)
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Custom Navigation Bar for Parents
        NavigationBar(
            containerColor = Color(0xFFF3EDF7),
            tonalElevation = 0.dp,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            NavigationBarItem(
                selected = activeTab == "HOME",
                onClick = { activeTab = "HOME" },
                icon = { Icon(Icons.Default.Home, contentDescription = "Ringkasan") },
                label = { Text("Utama", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            NavigationBarItem(
                selected = activeTab == "REQUEST",
                onClick = { activeTab = "REQUEST" },
                icon = { Icon(Icons.Default.Send, contentDescription = "Permintaan") },
                label = { Text("Izin", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            NavigationBarItem(
                selected = activeTab == "MONITOR",
                onClick = { activeTab = "MONITOR" },
                icon = { Icon(Icons.Default.List, contentDescription = "Log Monitor") },
                label = { Text("Pantau", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            NavigationBarItem(
                selected = activeTab == "MAP",
                onClick = { activeTab = "MAP" },
                icon = { Icon(Icons.Default.LocationOn, contentDescription = "Pelacak") },
                label = { Text("Lokasi", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            NavigationBarItem(
                selected = activeTab == "SETTINGS",
                onClick = { activeTab = "SETTINGS" },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Setelan") },
                label = { Text("Aturan", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }
    }
}

// --------------------------------------------------
// TAB 1: PARENT OVERVIEW DASHBOARD (UTAMA)
// --------------------------------------------------
@Composable
fun ParentHomeScreen(
    viewModel: ParentControlViewModel,
    config: ParentConfig
) {
    val requests by viewModel.allRequests.collectAsStateWithLifecycle()
    val logs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val simulatedHour by viewModel.simulatedHour.collectAsStateWithLifecycle()
    val isChildLocked = viewModel.isCurrentlyLocked(config, simulatedHour)
    val context = LocalContext.current

    val pendingRequestsCount = requests.count { it.status == "PENDING" }
    val warningLogsCount = logs.count { it.statusLevel == "WARN" || it.statusLevel == "DANGER" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and device status summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = config.childDeviceName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF21005D)
                        )
                        Text(
                            text = if (isChildLocked) "🔴 STATUS PERANGKAT: DIKUNCI" else "🟢 STATUS PERANGKAT: BEBAS/AKTIF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isChildLocked) Color(0xFFB3261E) else Color(0xFF10B981),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Large quick Force Lock Toggle
                    Button(
                        onClick = { viewModel.toggleManualLock() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (config.isManuallyLocked) Color(0xFF10B981) else Color(0xFF6750A4)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(if (config.isManuallyLocked) "Buka Kunci" else "Kunci HP Sekarang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Fast Stats Grid Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Permintaan Izin",
                    value = "$pendingRequestsCount Pending",
                    color = Color(0xFF3B82F6),
                    icon = Icons.Default.Send
                )

                OverviewStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Aktivitas Log",
                    value = "$warningLogsCount Peringatan",
                    color = Color(0xFFFBBF24),
                    icon = Icons.Default.Warning
                )
            }
        }

        // Active scheduling lock information card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚖️ Batasan & Proteksi Berlaku",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Batas Waktu Harian:", fontSize = 11.sp, color = Color.Gray)
                            Text("${config.dailyLimitMinutes} menit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        }
                        Column {
                            Text("Digunakan Hari Ini:", fontSize = 11.sp, color = Color.Gray)
                            Text("${config.minutesUsedToday} menit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                        }
                        Column {
                            Text("Curfew Jam Malam:", fontSize = 11.sp, color = Color.Gray)
                            Text("%s.00 - %s.00".format(config.bedtimeStartHour, config.bedtimeEndHour), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                }
            }
        }

        // Simulation Cheat Controllers (Essential for user testing in emulator)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7).copy(alpha = 0.70f)),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚙️ Panel Simulator Pengujian",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = "Gunakan tombol berikut untuk mempercepat simulasi dan melihat respons sistem proteksi.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.simulateTimeSpent(20)
                                Toast.makeText(context, "+20 menit disimulasikan!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("+20 Mnt Anak Pakai", fontSize = 10.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                viewModel.resetDailyTimeSpent()
                                Toast.makeText(context, "Presensi waktu pakai anak dicuci bersih!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Reset Pakai 0 Mnt", fontSize = 10.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated Custom Current Hour Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jam Virtual: %s.00".format(if (simulatedHour < 10) "0$simulatedHour" else "$simulatedHour"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier.width(110.dp)
                        )
                        Slider(
                            value = simulatedHour.toFloat(),
                            onValueChange = { viewModel.updateSimulatedHour(it.toInt()) },
                            valueRange = 0f..23f,
                            steps = 23,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "*(Geser jam di atas ke pukul %s.00 untuk mencoba kondisi jam malam istirahat, yang mana laptop/screen anak akan mengunci otomatis)".format(config.bedtimeStartHour),
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// --------------------------------------------------
// TAB 2: PERMISSION REQUESTS PROCESSING SCREEN (IZIN)
// --------------------------------------------------
@Composable
fun ParentRequestScreen(
    viewModel: ParentControlViewModel
) {
    val requests by viewModel.allRequests.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daftar Permohonan Izin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Izin tambahan screen-time dari HP anak dapat langsung disetujui di sini",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                IconButton(
                    onClick = { 
                        viewModel.clearAllRequests() 
                        Toast.makeText(context, "Log Semua Izin dibersihkan harian.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = Color.Gray)
                }
            }
        }

        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Belum ada permohonan dari anak", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(requests) { req ->
                    var isExpanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (req.status) {
                                                    "PENDING" -> Color(0xFFEFF6FF)
                                                    "APPROVED" -> Color(0xFFECFDF5)
                                                    else -> Color(0xFFFEF2F2)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (req.status) {
                                                "PENDING" -> Icons.Default.Send
                                                "APPROVED" -> Icons.Default.Check
                                                else -> Icons.Default.Close
                                            },
                                            contentDescription = null,
                                            tint = when (req.status) {
                                                "PENDING" -> Color(0xFF3B82F6)
                                                "APPROVED" -> Color(0xFF10B981)
                                                else -> Color(0xFFEF4444)
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "+${req.requestedMinutes} Menit Extra",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Badge(
                                    containerColor = when (req.status) {
                                        "PENDING" -> Color(0xFFDBEAFE)
                                        "APPROVED" -> Color(0xFFD1FAE5)
                                        else -> Color(0xFFFEE2E2)
                                    }
                                ) {
                                    Text(
                                        text = when(req.status) {
                                            "PENDING" -> "MENUNGGU"
                                            "APPROVED" -> "DISETUJUI"
                                            else -> "DITOLAK"
                                        },
                                        color = when(req.status) {
                                            "PENDING" -> Color(0xFF1E40AF)
                                            "APPROVED" -> Color(0xFF065F46)
                                            else -> Color(0xFF991B1B)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Alasan: \"${req.reason}\"",
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )

                            val df = SimpleDateFormat("HH:mm - dd MMM", Locale.getDefault())
                            Text(
                                text = df.format(Date(req.timestamp)),
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 6.dp)
                            )

                            // Show processing action buttons if status is PENDING
                            if (req.status == "PENDING") {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.rejectRequest(req.id) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tolak", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.approveRequest(req.id, req.requestedMinutes) },
                                        modifier = Modifier.weight(1.5f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Setujui Pemakaian", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------
// TAB 3: SAFETY MONITORING LOGS SCREEN (PANTAU)
// --------------------------------------------------
@Composable
fun ParentMonitorScreen(
    viewModel: ParentControlViewModel
) {
    val logs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Log Aktivitas Terpantau",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Riwayat penayangan, aplikasi dibuka, & notifikasi warning sistem harian anak",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = { 
                        viewModel.clearLogs() 
                        Toast.makeText(context, "Log dibersihkan", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }

        // Add custom log injection for parent to manually test warning events
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFEF2F2))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🚨 Tes Peringatan Instan:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            viewModel.addCustomMockLog(
                                app = "Google Search",
                                desc = "Anak mencoba mengakses website forum bebas kotor di luar aturan aman.",
                                level = "DANGER"
                            )
                            Toast.makeText(context, "Mencoba simulasi ancaman kotor!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Kotor Terblokir", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.addCustomMockLog(
                                app = "Instagram",
                                desc = "Percakapan mencurigakan terdeteksi mengandung unsur spamming atau penipuan.",
                                level = "WARN"
                            )
                            Toast.makeText(context, "Instagram warning disimulasikan!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Spam Sosial", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Belum ada log terekam hari ini", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (log.statusLevel) {
                                            "DANGER" -> Icons.Default.Warning
                                            "WARN" -> Icons.Default.Warning
                                            else -> Icons.Default.Check
                                        },
                                        contentDescription = null,
                                        tint = when (log.statusLevel) {
                                            "DANGER" -> Color(0xFFEF4444)
                                            "WARN" -> Color(0xFFFBBF24)
                                            else -> Color(0xFF10B981)
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.appName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                }

                                val logTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                Text(
                                    text = logTime,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = log.description,
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------
// TAB 4: SIMULATED GPS LOCATION TRACKER MAP (LOKASI)
// --------------------------------------------------
@Composable
fun ParentLocationScreen(
    viewModel: ParentControlViewModel
) {
    val activeLocation by viewModel.latestLocation.collectAsStateWithLifecycle()
    val locations by viewModel.locationLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lokasi Real-Time Anak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Pelacakan koordinat GPS silsilah area aman anak",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.clearLocations()
                        Toast.makeText(context, "Riwayat pelacakan dikosongkan.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clean Locations", tint = Color.Gray)
                }
            }
        }

        // Display the Canvas dynamic maps
        MockMap(
            activeLocation = activeLocation,
            modifier = Modifier.padding(16.dp)
        )

        // Switch location panel simulator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEDFBF7))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = "📡 SIMULASI PINDAH LOKASI PERANGKAT ANAK:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00796B)
                )
                Text(
                    text = "Pilih lokasi di bawah untuk memicu pemindahan kordinat dan memantau respons radar geofencing.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addCustomLocation(
                                placeName = "Area Rumah Tinggal",
                                lat = -6.1950,
                                lng = 106.8025,
                                accuracy = "Sinyal Bagus (GPS - 3m)"
                            )
                            Toast.makeText(context, "GPS diperbarui: HP Anak di Rumah 🏠", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("Ke Rumah 🏠", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.addCustomLocation(
                                placeName = "Sekolah Menengah Pertama 12",
                                lat = -6.1895,
                                lng = 106.8210,
                                accuracy = "Sinyal Wi-Fi (12m)"
                            )
                            Toast.makeText(context, "GPS diperbarui: HP Anak di Sekolah 🏫", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("Ke Sekolah 🏫", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.addCustomLocation(
                                placeName = "Mal Plaza Metropolitan",
                                lat = -6.1824,
                                lng = 106.8294,
                                accuracy = "Sinyal Seluler (15m)"
                            )
                            Toast.makeText(context, "GPS diperbarui: HP Anak di Luar Zona Aman ⚠️", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("Ke Mal (Luar Area) ⚠️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent Tracking Logs list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            item {
                Text(
                    text = "Riwayat Koordinat Terakhir:",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(locations) { loc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (loc.placeName.contains("Luar") || loc.placeName.contains("Mal")) Color.Red else Color(0xFF00796B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = loc.placeName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    val locTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(loc.timestamp))
                    Text(text = locTime, fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

// --------------------------------------------------
// TAB 5: CURFEWS & PIN CONFIGURATION SETTINGS SCREEN
// --------------------------------------------------
@Composable
fun ParentSettingsScreen(
    viewModel: ParentControlViewModel,
    config: ParentConfig
) {
    var bedtimeStart by remember { mutableStateOf(config.bedtimeStartHour) }
    var bedtimeEnd by remember { mutableStateOf(config.bedtimeEndHour) }
    var dailyLimit by remember { mutableStateOf(config.dailyLimitMinutes) }

    var passcodeEditingValue by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Rules Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚖️ Manajemen Aturan Batas Layar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Tentukan durasi penggunaan harian gadget anak, jika tercapai hp akan dikunci",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Slider Limit
                Text(
                    text = "Batas Aktivitas Harian: $dailyLimit Menit (%d Jam %d Menit)".format(dailyLimit / 60, dailyLimit % 60),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                
                Slider(
                    value = dailyLimit.toFloat(),
                    onValueChange = { dailyLimit = it.toInt() },
                    valueRange = 15f..360f,
                    steps = 23,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Button(
                    onClick = {
                        viewModel.updateDailyLimit(dailyLimit)
                        Toast.makeText(context, "Aturan batas harian disimpan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Terapkan Batas Waktu", fontSize = 12.sp)
                }
            }
        }

        // Curfew Scheduled curfew
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💤 Jadwal Jam Malam Istirahat (Curfew)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Layar HP anak terkunci otomatis dalam rentang waktu tidur harian",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Choose curfew Start
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Jam Istirahat Dimulai: %s.00".format(if (bedtimeStart < 10) "0$bedtimeStart" else "$bedtimeStart"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (bedtimeStart > 0) bedtimeStart-- }) {
                            Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { if (bedtimeStart < 23) bedtimeStart++ }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }

                // Choose curfew End
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Jam Istirahat Selesai: %s.00".format(if (bedtimeEnd < 10) "0$bedtimeEnd" else "$bedtimeEnd"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (bedtimeEnd > 0) bedtimeEnd-- }) {
                            Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { if (bedtimeEnd < 23) bedtimeEnd++ }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.updateBedtimeSchedule(bedtimeStart, bedtimeEnd)
                        Toast.makeText(context, "Jadwal curfew jam tidur disimpan!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Terapkan Curfew Malam", fontSize = 12.sp)
                }
            }
        }

        // Change Security PIN Code
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 Kode PIN Keamanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Ubah kode PIN 4 digit untuk menjaga akses dashboard agar tidak dimasuki anak",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "PIN Sekarang: ${config.passcode}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E88E5)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = passcodeEditingValue,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            passcodeEditingValue = it
                        }
                    },
                    label = { Text("Masukkan PIN 4 digit Baru", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (passcodeEditingValue.length != 4) {
                            Toast.makeText(context, "PIN baru harus bertotal tepat 4 angka!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updatePasscode(passcodeEditingValue)
                            Toast.makeText(context, "PIN Keamanan sukses diubah!", Toast.LENGTH_SHORT).show()
                            passcodeEditingValue = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update PIN Aturan", fontSize = 12.sp)
                }
            }
        }
    }
}
