package com.example.newtes // Sesuaikan dengan package name Anda

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.newtes.ui.theme.NewTesTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

// --- DATA DUMMY ---
const val OFFICE_LATITUDE = -6.898382687844024
const val OFFICE_LONGITUDE = 107.63881370142434
const val MAX_DISTANCE_METERS = 500.0
const val CORRECT_QR_ID = "SITEKAD-GS-01"

data class AttendanceRecord(
    val date: String, val day: String, val clockIn: String, val clockOut: String, val isLate: Boolean = false
)

sealed class Screen(val route: String) {
    object Home : Screen("home_main")
    object Lembur : Screen("lembur")
    object Profile : Screen("profile")
    object ConfirmAttendance : Screen("confirm_attendance/{scanResult}/{attendanceType}") {
        fun createRoute(scanResult: String, attendanceType: String): String {
            val encodedResult = URLEncoder.encode(scanResult, StandardCharsets.UTF_8.toString())
            return "confirm_attendance/$encodedResult/$attendanceType"
        }
    }
}

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val username = intent.getStringExtra("EXTRA_USERNAME") ?: "Pengguna"
        setContent {
            NewTesTheme {
                MainAppScreen(username = username)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(username: String) {
    val navController = rememberNavController()
    // --- STATE UTAMA ABSEN REGULER ---
    var attendanceStatus by rememberSaveable { mutableStateOf("Belum Absen Hari Ini") }
    var isClockedIn by rememberSaveable { mutableStateOf(false) }
    var isClockedOut by rememberSaveable { mutableStateOf(false) }
    var clockInTime by rememberSaveable { mutableStateOf("--:--") }
    val attendanceHistory = remember { mutableStateListOf<AttendanceRecord>() }

    // --- STATE UTAMA ABSEN LEMBUR ---
    var lemburStatus by rememberSaveable { mutableStateOf("Belum Lembur Hari Ini") }
    var isLemburClockedIn by rememberSaveable { mutableStateOf(false) }
    var isLemburClockedOut by rememberSaveable { mutableStateOf(false) }
    var lemburClockInTime by rememberSaveable { mutableStateOf("--:--") }
    val lemburHistory = remember { mutableStateListOf<AttendanceRecord>() }
    var fileUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var fileName by rememberSaveable { mutableStateOf<String?>(null) }
    var isSplSubmitted by rememberSaveable { mutableStateOf(false) }


    val bottomNavItems = listOf(
        Screen.Home, Screen.Lembur, Screen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 5.dp) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (item) {
                                is Screen.Home -> Icon(Icons.Default.Home, contentDescription = "Home")
                                is Screen.Lembur -> Icon(Icons.Default.Notifications, contentDescription = "Lembur")
                                is Screen.Profile -> Icon(Icons.Default.Person, contentDescription = "Profile")
                                else -> {}
                            }
                        },
                        label = {
                            when (item) {
                                is Screen.Home -> Text("Home")
                                is Screen.Lembur -> Text("Lembur")
                                is Screen.Profile -> Text("Profile")
                                else -> {}
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreenContent(
                    username = username,
                    navController = navController,
                    attendanceStatus = attendanceStatus,
                    isClockedIn = isClockedIn,
                    isClockedOut = isClockedOut,
                    history = attendanceHistory
                )
            }
            composable(
                route = Screen.ConfirmAttendance.route,
                arguments = listOf(
                    navArgument("scanResult") { type = NavType.StringType },
                    navArgument("attendanceType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val scanResult = URLDecoder.decode(backStackEntry.arguments?.getString("scanResult") ?: "", StandardCharsets.UTF_8.toString())
                val attendanceType = backStackEntry.arguments?.getString("attendanceType") ?: "in"

                ConfirmationScreen(
                    navController = navController,
                    qrCodeId = scanResult,
                    attendanceType = attendanceType,
                    onConfirm = { time, id ->
                        when (attendanceType) {
                            "in" -> {
                                attendanceStatus = "Hadir - Masuk pukul $time"
                                isClockedIn = true
                                clockInTime = time
                            }
                            "out" -> {
                                val newRecord = AttendanceRecord(
                                    date = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()),
                                    day = SimpleDateFormat("E", Locale("id", "ID")).format(Date()),
                                    clockIn = clockInTime,
                                    clockOut = time,
                                    isLate = false
                                )
                                attendanceHistory.add(0, newRecord)
                                isClockedOut = true
                                attendanceStatus = "Anda sudah absen hari ini."
                            }
                            "in-lembur" -> {
                                lemburStatus = "Lembur - Masuk pukul $time"
                                isLemburClockedIn = true
                                lemburClockInTime = time
                            }
                            "out-lembur" -> {
                                val newRecord = AttendanceRecord(
                                    date = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()),
                                    day = SimpleDateFormat("E", Locale("id", "ID")).format(Date()),
                                    clockIn = lemburClockInTime,
                                    clockOut = time,
                                    isLate = false
                                )
                                lemburHistory.add(0, newRecord)
                                isLemburClockedOut = true
                                lemburStatus = "Anda sudah selesai lembur hari ini."
                            }
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Lembur.route) {
                val context = LocalContext.current
                LemburScreen(
                    username = username,
                    navController = navController,
                    lemburStatus = lemburStatus,
                    isLemburClockedIn = isLemburClockedIn,
                    isLemburClockedOut = isLemburClockedOut,
                    history = lemburHistory,
                    fileName = fileName,
                    isSplSubmitted = isSplSubmitted,
                    onFileSelected = { uri ->
                        fileUriString = uri.toString()
                        fileName = getFileName(context, uri)
                        isSplSubmitted = false
                    },
                    onSplSubmit = { isSplSubmitted = true }
                )
            }
            composable(Screen.Profile.route) { PlaceholderScreen(text = "Halaman Profil Pengguna") }
        }
    }
}


@Composable
fun HomeScreenContent(
    username: String,
    navController: NavHostController,
    attendanceStatus: String,
    isClockedIn: Boolean,
    isClockedOut: Boolean,
    history: List<AttendanceRecord>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            UserHeader(username = username)
            Spacer(modifier = Modifier.height(32.dp))
        }
        item {
            ClockSection(navController, attendanceStatus, isClockedIn, isClockedOut)
            Spacer(modifier = Modifier.height(32.dp))
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.History, contentDescription = "History Icon", tint = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attendance History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(history) { record ->
            HistoryItem(record = record)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ClockSection(
    navController: NavHostController,
    attendanceStatus: String,
    isClockedIn: Boolean,
    isClockedOut: Boolean,
    isLembur: Boolean = false
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("--:--:--") }
    var currentDate by remember { mutableStateOf("") }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            if (result.contents != null) {
                val type = if (!isClockedIn) {
                    if (isLembur) "in-lembur" else "in"
                } else {
                    if (isLembur) "out-lembur" else "out"
                }
                navController.navigate(Screen.ConfirmAttendance.createRoute(result.contents, type))
            } else {
                Toast.makeText(context, "Scan Dibatalkan", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val localeId = Locale("id", "ID")
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", localeId)
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = currentTime, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = currentDate, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Status Kehadiran Anda:", color = MaterialTheme.colorScheme.secondary)
        Text(
            text = attendanceStatus,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isClockedIn && !isClockedOut) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    val options = ScanOptions()
                    options.setPrompt("Arahkan kamera ke QR Code Absensi")
                    options.setBeepEnabled(true)
                    options.setOrientationLocked(true)
                    scanLauncher.launch(options)
                },
                modifier = Modifier.weight(1f).height(50.dp),
                enabled = !isClockedIn,
                shape = RoundedCornerShape(12.dp)
            ) { Text(if(isLembur) "Clock In Lembur" else "Clock In", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = {
                    val options = ScanOptions()
                    options.setPrompt("Arahkan kamera ke QR Code Absensi")
                    options.setBeepEnabled(true)
                    options.setOrientationLocked(true)
                    scanLauncher.launch(options)
                },
                modifier = Modifier.weight(1f).height(50.dp),
                enabled = isClockedIn && !isClockedOut,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) { Text(if(isLembur) "Clock Out Lembur" else "Clock Out", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}


@Composable
fun ConfirmationScreen(
    navController: NavHostController,
    qrCodeId: String,
    attendanceType: String,
    onConfirm: (String, String) -> Unit
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var locationFetchStatus by remember { mutableStateOf("Meminta izin lokasi...") }
    val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    val dateNow = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                locationFetchStatus = "Mencari lokasi Anda..."
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            currentLocation = location
                            locationFetchStatus = "Lokasi ditemukan!"
                        } else {
                            locationFetchStatus = "Gagal mendapatkan lokasi."
                        }
                    }
            } else {
                locationFetchStatus = "Izin lokasi ditolak."
                Toast.makeText(context, "Aplikasi memerlukan izin lokasi untuk absen", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        }
    )

    LaunchedEffect(Unit) {
        val permissionsToRequest = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            locationFetchStatus = "Mencari lokasi Anda..."
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLocation = location
                        locationFetchStatus = "Lokasi ditemukan!"
                    } else {
                        locationFetchStatus = "Gagal mendapatkan lokasi."
                    }
                }
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (attendanceType) {
                    "in" -> "Konfirmasi Clock In"
                    "out" -> "Konfirmasi Clock Out"
                    "in-lembur" -> "Konfirmasi Clock In Lembur"
                    "out-lembur" -> "Konfirmasi Clock Out Lembur"
                    else -> "Konfirmasi Absensi"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Waktu", color = MaterialTheme.colorScheme.secondary); Text(timeNow, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tanggal", color = MaterialTheme.colorScheme.secondary); Text(dateNow, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ID Lokasi", color = MaterialTheme.colorScheme.secondary); Text(qrCodeId, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status Lokasi", color = MaterialTheme.colorScheme.secondary); Text(locationFetchStatus, fontWeight = FontWeight.Bold, color = if (currentLocation != null) Color(0xFF2ECC71) else MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = {
                    if (currentLocation == null) {
                        Toast.makeText(context, "Gagal mendapatkan lokasi. Pastikan GPS aktif.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (qrCodeId != CORRECT_QR_ID) {
                        Toast.makeText(context, "Gagal: QR Code lokasi tidak valid!", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val distanceResults = FloatArray(1)
                    Location.distanceBetween(
                        currentLocation!!.latitude, currentLocation!!.longitude,
                        OFFICE_LATITUDE, OFFICE_LONGITUDE,
                        distanceResults
                    )
                    val distanceInMeters = distanceResults[0]
                    if (distanceInMeters <= MAX_DISTANCE_METERS) {
                        onConfirm(timeNow, qrCodeId)
                    } else {
                        Toast.makeText(context, "Gagal: Anda berada terlalu jauh dari kantor! (Jarak: ${distanceInMeters.toInt()} meter)", Toast.LENGTH_LONG).show()
                    }
                },
                enabled = currentLocation != null,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (currentLocation == null) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Kirim Absen", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { navController.popBackStack() }) { Text("Batal", color = MaterialTheme.colorScheme.secondary) }
        }
    }
}


@Composable
fun LemburScreen(
    username: String,
    navController: NavHostController,
    lemburStatus: String,
    isLemburClockedIn: Boolean,
    isLemburClockedOut: Boolean,
    history: List<AttendanceRecord>,
    fileName: String?,
    isSplSubmitted: Boolean,
    onFileSelected: (Uri) -> Unit,
    onSplSubmit: () -> Unit
) {
    var isUploading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                onFileSelected(it)
            }
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            UserHeader(username = username)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Pengajuan Lembur", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text("1. Upload Surat Perintah Lembur (SPL)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            FilePickerBox(fileName = fileName, onClick = { if (!isUploading) filePickerLauncher.launch("*/*") })
        }

        if (fileName != null && !isSplSubmitted) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isUploading = true
                            delay(2000)
                            onSplSubmit()
                            isUploading = false
                            Toast.makeText(context, "SPL berhasil dikirim!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Kirim SPL")
                    }
                }
            }
        }

        if (isSplSubmitted) {
            item {
                Divider(modifier = Modifier.padding(vertical = 32.dp))
                Text("2. Lakukan Absensi Lembur", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                ClockSection(
                    navController = navController,
                    attendanceStatus = lemburStatus,
                    isClockedIn = isLemburClockedIn,
                    isClockedOut = isLemburClockedOut,
                    isLembur = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = "History Icon", tint = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Riwayat Lembur", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(history) { record ->
                HistoryItem(record = record)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}


@Composable
fun FilePickerBox(fileName: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (fileName == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Upload Icon", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Klik untuk memilih file SPL\n(Gambar atau PDF)", color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = "File Icon", tint = Color(0xFF2ECC71), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("File Terpilih:", color = MaterialTheme.colorScheme.secondary)
                Text(
                    text = fileName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var fileName: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
    }
}

@Composable
fun UserHeader(username: String) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "User Avatar", modifier = Modifier.size(50.dp).clip(CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = username, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "27738749 - Senior UX Designer", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { (context as? Activity)?.finish() }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun HistoryItem(record: AttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(record.day, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                Text(record.date, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${record.clockIn} - ${record.clockOut}",
                color = if (record.isLate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}