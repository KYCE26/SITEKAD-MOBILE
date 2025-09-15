// --- GANTI SELURUH BLOK IMPORT ANDA DENGAN INI ---

package com.example.newtes // Sesuaikan dengan package name Anda

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.newtes.ui.theme.NewTesTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

// --- DATA DUMMY (HANYA UNTUK VALIDASI FRONTEND) ---
const val MAX_DISTANCE_METERS = 500.0

data class UserProfile(
    val username: String = "...",
    val nitad: String = "...",
    val namaLengkap: String = "...",
    val jabatan: String = "...",
    val cabang: String = "...",
    val lokasi: String = "..."
)
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
    val context = LocalContext.current
    val activity = (context as? Activity)

    val userProfile = remember { mutableStateOf<UserProfile?>(null) }

    // --- STATE UTAMA ABSEN REGULER ---
    val attendanceStatus = rememberSaveable { mutableStateOf("Belum Absen Hari Ini") }
    val isClockedIn = rememberSaveable { mutableStateOf(false) }
    val isClockedOut = rememberSaveable { mutableStateOf(false) }
    val clockInTime = rememberSaveable { mutableStateOf("--:--") }
    val attendanceHistory = remember { mutableStateListOf<AttendanceRecord>() }

    // --- STATE UTAMA ABSEN LEMBUR ---
    val lemburStatus = rememberSaveable { mutableStateOf("Belum Lembur Hari Ini") }
    val isLemburClockedIn = rememberSaveable { mutableStateOf(false) }
    val isLemburClockedOut = rememberSaveable { mutableStateOf(false) }
    val lemburClockInTime = rememberSaveable { mutableStateOf("--:--") }
    val lemburHistory = remember { mutableStateListOf<AttendanceRecord>() }
    val fileUriString = rememberSaveable { mutableStateOf<String?>(null) }
    val fileName = rememberSaveable { mutableStateOf<String?>(null) }
    val isSplSubmitted = rememberSaveable { mutableStateOf(false) }


    val bottomNavItems = listOf(
        Screen.Home, Screen.Lembur, Screen.Profile
    )

    LaunchedEffect(Unit) {
        val url = "http://202.138.248.93:10084/api/profile"
        val requestQueue = Volley.newRequestQueue(context)
        val jsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val profileJson = response.getJSONObject("profile")
                    userProfile.value = UserProfile(
                        username = profileJson.getString("Username"),
                        nitad = profileJson.getString("NITAD"),
                        namaLengkap = profileJson.getString("Nama Lengkap"),
                        jabatan = profileJson.getString("Jabatan"),
                        cabang = profileJson.getString("Cabang"),
                        lokasi = profileJson.getString("Lokasi")
                    )
                } catch (e: Exception) {
                    Log.e("ProfileAPI", "Gagal parsing profil: ${e.message}")
                }
            },
            { error -> Log.e("ProfileAPI", "Gagal mengambil profil: ${error.message}") }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val sharedPreferences = context.getSharedPreferences("SITEKAD_PREFS", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("jwt_token", null)
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    fun doLogout() {
        val sharedPreferences = context.getSharedPreferences("SITEKAD_PREFS", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", null)
        if (token != null) {
            val logoutUrl = "http://202.138.248.93:10084/logout"
            val requestQueue = Volley.newRequestQueue(context)
            val logoutRequest = object: JsonObjectRequest(
                Request.Method.GET, logoutUrl, null,
                { _ -> Log.d("LOGOUT_API", "Logout API success") },
                { error -> Log.e("LOGOUT_API", "Logout API error: ${error.message}") }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer $token"
                    return headers
                }
            }
            requestQueue.add(logoutRequest)
        }
        sharedPreferences.edit().clear().apply()
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        activity?.finish()
    }

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
                                is Screen.Home -> Icon(Icons.Filled.Home, contentDescription = "Home")
                                is Screen.Lembur -> Icon(Icons.Filled.Notifications, contentDescription = "Lembur")
                                is Screen.Profile -> Icon(Icons.Filled.Person, contentDescription = "Profile")
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
                        }
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
                    userProfile = userProfile.value,
                    navController = navController,
                    attendanceStatus = attendanceStatus,
                    isClockedIn = isClockedIn,
                    isClockedOut = isClockedOut,
                    history = attendanceHistory,
                    onLogoutClick = { doLogout() }
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
                    onConfirm = { time, _ ->
                        when (attendanceType) {
                            "in" -> {
                                attendanceStatus.value = "Hadir - Masuk pukul $time"
                                isClockedIn.value = true
                                isClockedOut.value = false
                                clockInTime.value = time
                            }
                            "out" -> {
                                val newRecord = AttendanceRecord(
                                    date = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()),
                                    day = SimpleDateFormat("E", Locale("id", "ID")).format(Date()),
                                    clockIn = clockInTime.value,
                                    clockOut = time,
                                    isLate = false
                                )
                                attendanceHistory.add(0, newRecord)
                                isClockedOut.value = true
                                attendanceStatus.value = "Anda sudah absen hari ini."
                            }
                            "in-lembur" -> {
                                lemburStatus.value = "Lembur - Masuk pukul $time"
                                isLemburClockedIn.value = true
                                isLemburClockedOut.value = false
                                lemburClockInTime.value = time
                            }
                            "out-lembur" -> {
                                val newRecord = AttendanceRecord(
                                    date = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()),
                                    day = SimpleDateFormat("E", Locale("id", "ID")).format(Date()),
                                    clockIn = lemburClockInTime.value,
                                    clockOut = time,
                                    isLate = false
                                )
                                lemburHistory.add(0, newRecord)
                                isLemburClockedOut.value = true
                                lemburStatus.value = "Anda sudah selesai lembur hari ini."
                            }
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Lembur.route) {
                LemburScreen(
                    userProfile = userProfile.value,
                    navController = navController,
                    lemburStatus = lemburStatus.value,
                    isLemburClockedIn = isLemburClockedIn.value,
                    isLemburClockedOut = isLemburClockedOut.value,
                    history = lemburHistory,
                    fileName = fileName.value,
                    fileUriString = fileUriString.value,
                    isSplSubmitted = isSplSubmitted.value,
                    onFileSelected = { uri ->
                        fileUriString.value = uri.toString()
                        fileName.value = getFileName(context, uri)
                        isSplSubmitted.value = false
                    },
                    onSplSubmit = { isSplSubmitted.value = true },
                    onLogoutClick = { doLogout() },
                    onClearFile = {
                        fileUriString.value = null
                        fileName.value = null
                        isSplSubmitted.value = false
                    }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    userProfile = userProfile.value,
                    onLogoutClick = { doLogout() }
                )
            }
        }
    }
}


@Composable
fun HomeScreenContent(
    userProfile: UserProfile?,
    navController: NavHostController,
    attendanceStatus: MutableState<String>,
    isClockedIn: MutableState<Boolean>,
    isClockedOut: MutableState<Boolean>,
    history: MutableList<AttendanceRecord>,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (history.isEmpty()) {
            val url = "http://202.138.248.93:10084/api/uhistori"
            val requestQueue = Volley.newRequestQueue(context)
            val jsonObjectRequest = object : JsonObjectRequest(
                Request.Method.GET, url, null,
                { response ->
                    try {
                        isLoading = false
                        val historyArray = response.getJSONArray("history")
                        val records = mutableListOf<AttendanceRecord>()
                        for (i in 0 until historyArray.length()) {
                            val item = historyArray.getJSONObject(i)
                            fun formatDate(dateString: String): String {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                                return try {
                                    inputFormat.parse(dateString)?.let { outputFormat.format(it) } ?: dateString
                                } catch (e: Exception) { dateString }
                            }
                            fun formatDay(dateString: String): String {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                                val outputFormat = SimpleDateFormat("E", Locale("id", "ID"))
                                return try {
                                    inputFormat.parse(dateString)?.let { outputFormat.format(it) } ?: ""
                                } catch (e: Exception) { "" }
                            }
                            records.add(
                                AttendanceRecord(
                                    date = formatDate(item.getString("tgl_absen")),
                                    day = formatDay(item.getString("tgl_absen")),
                                    clockIn = item.getString("jam_masuk"),
                                    clockOut = item.optString("jam_keluar", "null"), // Ambil sebagai string "null" jika kosong
                                    isLate = false
                                )
                            )
                        }
                        history.clear()
                        history.addAll(records)

                        val todayDateString = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
                        val latestRecord = history.firstOrNull()

                        if (latestRecord != null && latestRecord.date == todayDateString) {
                            if (latestRecord.clockOut != "null") { // Cek apakah sudah clock out
                                attendanceStatus.value = "Anda sudah absen hari ini."
                                isClockedIn.value = true
                                isClockedOut.value = true
                            } else {
                                attendanceStatus.value = "Hadir - Masuk pukul ${latestRecord.clockIn}"
                                isClockedIn.value = true
                                isClockedOut.value = false
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = "Gagal memproses data: ${e.message}"
                        isLoading = false
                    }
                },
                { error ->
                    isLoading = false
                    errorMessage = error.networkResponse?.let {
                        val errorData = String(it.data, Charsets.UTF_8)
                        "Error ${it.statusCode}: $errorData"
                    } ?: "Error: ${error.message}"
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    val sharedPreferences = context.getSharedPreferences("SITEKAD_PREFS", Context.MODE_PRIVATE)
                    val token = sharedPreferences.getString("jwt_token", null)
                    if (token != null) {
                        headers["Authorization"] = "Bearer $token"
                    }
                    return headers
                }
            }
            requestQueue.add(jsonObjectRequest)
        } else {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            UserHeader(userProfile = userProfile, onLogoutClick = onLogoutClick)
            Spacer(modifier = Modifier.height(32.dp))
        }
        item {
            ClockSection(navController, attendanceStatus.value, isClockedIn.value, isClockedOut.value)
            Spacer(modifier = Modifier.height(32.dp))
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.History, contentDescription = "History Icon", tint = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attendance History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        when {
            isLoading -> {
                item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
            errorMessage != null -> {
                item { Text(text = "Gagal memuat riwayat:\n$errorMessage", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            }
            history.isEmpty() -> {
                item { Text(text = "Belum ada riwayat absensi.", color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center) }
            }
            else -> {
                items(history) { record ->
                    HistoryItem(record = record)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
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
                val idLokasi = result.contents
                val type = if (!isClockedIn) {
                    if (isLembur) "in-lembur" else "in"
                } else {
                    if (isLembur) "out-lembur" else "out"
                }
                navController.navigate(Screen.ConfirmAttendance.createRoute(idLokasi, type))
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
            Button(
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                )
            ) { Text(if(isLembur) "Clock Out Lembur" else "Clock Out", fontWeight = FontWeight.Bold) }
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
    var isSubmitting by remember { mutableStateOf(false) }
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
                    isSubmitting = true
                    val requestQueue = Volley.newRequestQueue(context)
                    val url = "http://202.138.248.93:10084/api/absensi"

                    val stringRequest = object : StringRequest(
                        Method.POST, url,
                        { response ->
                            isSubmitting = false
                            Toast.makeText(context, "Absen Berhasil: $response", Toast.LENGTH_SHORT).show()
                            onConfirm(timeNow, qrCodeId)
                        },
                        { error ->
                            isSubmitting = false
                            val errorMessage = error.networkResponse?.let { String(it.data, Charsets.UTF_8) } ?: "Error: ${error.message}"
                            Toast.makeText(context, "Gagal mengirim absen: $errorMessage", Toast.LENGTH_LONG).show()
                        }) {

                        override fun getHeaders(): MutableMap<String, String> {
                            val headers = HashMap<String, String>()
                            val sharedPreferences = context.getSharedPreferences("SITEKAD_PREFS", Context.MODE_PRIVATE)
                            val token = sharedPreferences.getString("jwt_token", "")
                            headers["Authorization"] = "Bearer $token"
                            return headers
                        }

                        override fun getBodyContentType() = "application/json; charset=utf-8"
                        override fun getBody(): ByteArray {
                            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                            val jsonBody = JSONObject()
                            jsonBody.put("latitude", currentLocation!!.latitude)
                            jsonBody.put("longitude", currentLocation!!.longitude)
                            jsonBody.put("android_id", androidId)
                            jsonBody.put("kodeqr", qrCodeId)
                            return jsonBody.toString().toByteArray(Charsets.UTF_8)
                        }
                    }
                    requestQueue.add(stringRequest)
                },
                enabled = currentLocation != null && !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSubmitting || currentLocation == null) {
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
    userProfile: UserProfile?,
    navController: NavHostController,
    lemburStatus: String,
    isLemburClockedIn: Boolean,
    isLemburClockedOut: Boolean,
    history: List<AttendanceRecord>,
    fileName: String?,
    fileUriString: String?,
    isSplSubmitted: Boolean,
    onFileSelected: (Uri) -> Unit,
    onSplSubmit: () -> Unit,
    onLogoutClick: () -> Unit,
    onClearFile: () -> Unit
) {
    var isUploading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val fileUri = fileUriString?.toUri()

    // --- STATE BARU UNTUK DIALOG PREVIEW ---
    var showImagePreviewDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                onFileSelected(it)
            }
        }
    )

    // --- DIALOG UNTUK MENAMPILKAN GAMBAR UKURAN PENUH ---
    if (showImagePreviewDialog && fileUri != null) {
        Dialog(onDismissRequest = { showImagePreviewDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                AsyncImage(
                    model = fileUri,
                    contentDescription = "Full Screen Preview",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            UserHeader(userProfile = userProfile, onLogoutClick = onLogoutClick)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Pengajuan Lembur", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text("1. Upload Surat Perintah Lembur (SPL)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            FilePickerBox(
                fileUri = fileUri,
                onClick = {
                    if (fileUri == null) {
                        imagePickerLauncher.launch("image/*") // Jika kosong, buka galeri
                    } else {
                        showImagePreviewDialog = true // Jika terisi, buka dialog
                    }
                },

                    onClearImage = onClearFile) // Kirim Uri kosong untuk mereset
                }



        if (fileUri != null && !isSplSubmitted) {
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
                    Icon(imageVector = Icons.Filled.History, contentDescription = "History Icon", tint = MaterialTheme.colorScheme.onBackground)
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
fun FilePickerBox(
    fileUri: Uri?,
    onClick: () -> Unit,
    onClearImage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick) // Aksi klik utama (buka galeri/dialog)
    ) {
        if (fileUri == null || fileUri.toString().isEmpty()) {
            // Tampilan default jika belum ada gambar
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = "Upload Icon", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Klik untuk memilih gambar SPL", color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
            }
        } else {
            // Tampilan jika gambar sudah dipilih
            AsyncImage(
                model = fileUri,
                contentDescription = "Preview SPL",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // Crop agar memenuhi box
            )
            // Tombol Hapus (ikon 'X') di pojok kanan atas
            IconButton(
                onClick = onClearImage,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus Gambar", tint = MaterialTheme.colorScheme.onSurface)
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
fun UserHeader(userProfile: UserProfile?, onLogoutClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "User Avatar", modifier = Modifier.size(50.dp).clip(CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = userProfile?.namaLengkap ?: "Memuat...", // Tampilkan nama lengkap
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Tampilkan NITAD dan Jabatan
            Text(
                text = "${userProfile?.nitad ?: ""} - ${userProfile?.jabatan ?: ""}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onLogoutClick) {
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
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = "Tanggal",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.day}, ${record.date}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${record.clockIn} - ${record.clockOut}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (record.isLate) MaterialTheme.colorScheme.primary else Color(0xFF2ECC71)
                    )
            )
        }
    }
}

// Di dalam file HomeActivity.kt

@Composable
fun ProfileScreen(userProfile: UserProfile?, onLogoutClick: () -> Unit) {
    val context = LocalContext.current
    var isLoggingOut by remember { mutableStateOf(false) }

    // Hapus LaunchedEffect dari sini karena data sudah diambil di MainAppScreen

    Surface(modifier = Modifier.fillMaxSize()) {
        if (userProfile == null) { // Tampilkan loading jika userProfile masih null
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Tampilan jika data berhasil dimuat
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Bagian Header Profil ---
                Spacer(modifier = Modifier.height(32.dp))
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "User Avatar",
                    modifier = Modifier.size(120.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = userProfile.namaLengkap, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "NITAD: ${userProfile.nitad}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(32.dp))

                // --- Bagian Detail Informasi ---
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileInfoItem(icon = Icons.Filled.Work, label = "Jabatan", value = userProfile.jabatan)
                    ProfileInfoItem(icon = Icons.Filled.Business, label = "Cabang", value = userProfile.cabang)
                    ProfileInfoItem(icon = Icons.Filled.LocationOn, label = "Lokasi", value = userProfile.lokasi)
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- Tombol Logout ---
                Button(
                    onClick = {
                        isLoggingOut = true
                        onLogoutClick()
                    },
                    enabled = !isLoggingOut,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Logout", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
