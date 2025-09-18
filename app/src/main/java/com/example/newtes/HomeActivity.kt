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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

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
data class LemburResponse(
    val message: String,
    val file_disimpan: String
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
        setContent {
            NewTesTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = (context as? Activity)
    val userProfile = remember { mutableStateOf<UserProfile?>(null) }

    val attendanceStatus = rememberSaveable { mutableStateOf("Belum Absen Hari Ini") }
    val isClockedIn = rememberSaveable { mutableStateOf(false) }
    val isClockedOut = rememberSaveable { mutableStateOf(false) }
    val clockInTime = rememberSaveable { mutableStateOf("--:--") }
    val attendanceHistory = remember { mutableStateListOf<AttendanceRecord>() }

    val lemburStatus = rememberSaveable { mutableStateOf("Belum Lembur Hari Ini") }
    val isLemburClockedIn = rememberSaveable { mutableStateOf(false) }
    val isLemburClockedOut = rememberSaveable { mutableStateOf(false) }
    val lemburClockInTime = rememberSaveable { mutableStateOf("--:--") }
    val lemburHistory = remember { mutableStateListOf<AttendanceRecord>() }
    val fileUriString = rememberSaveable { mutableStateOf<String?>(null) }

    val bottomNavItems = listOf(Screen.Home, Screen.Lembur, Screen.Profile)

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
                                is Screen.Home -> Icon(Icons.Filled.Home, "Home")
                                is Screen.Lembur -> Icon(Icons.Filled.Notifications, "Lembur")
                                is Screen.Profile -> Icon(Icons.Filled.Person, "Profile")
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
                    isClockedIn = isClockedIn.value,
                    isClockedOut = isClockedOut.value,
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
                    fileUriString = fileUriString.value,
                    onConfirm = { time, _ ->
                        when (attendanceType) {
                            "in" -> {
                                attendanceStatus.value = "Hadir - Masuk pukul $time"
                                isClockedIn.value = true
                                isClockedOut.value = false
                                clockInTime.value = time
                            }
                            "out" -> {
                                val newRecord = AttendanceRecord(SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()), SimpleDateFormat("E", Locale("id", "ID")).format(Date()), clockInTime.value, time)
                                attendanceHistory.add(0, newRecord)
                                isClockedOut.value = true
                                attendanceStatus.value = "Anda sudah absen hari ini."
                            }
                            "in-lembur" -> {
                                lemburStatus.value = "Lembur - Masuk pukul $time"
                                isLemburClockedIn.value = true
                                isLemburClockedOut.value = false
                                lemburClockInTime.value = time
                                fileUriString.value = null
                            }
                            "out-lembur" -> {
                                val newRecord = AttendanceRecord(SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()), SimpleDateFormat("E", Locale("id", "ID")).format(Date()), lemburClockInTime.value, time)
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
                    fileUriString = fileUriString.value,
                    onFileSelected = { uri -> fileUriString.value = uri.toString() },
                    onLogoutClick = { doLogout() },
                    onClearFile = { fileUriString.value = null }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(userProfile = userProfile.value, onLogoutClick = { doLogout() })
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    userProfile: UserProfile?,
    navController: NavHostController,
    attendanceStatus: MutableState<String>,
    isClockedIn: Boolean,
    isClockedOut: Boolean,
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
                                    clockOut = item.optString("jam_keluar", "null"),
                                    isLate = false
                                )
                            )
                        }
                        history.clear()
                        history.addAll(records)
                        val todayDateString = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
                        val latestRecord = history.firstOrNull()
                        if (latestRecord != null && latestRecord.date == todayDateString) {
                            if (latestRecord.clockOut != "null") {
                                attendanceStatus.value = "Anda sudah absen hari ini."
                            } else {
                                attendanceStatus.value = "Hadir - Masuk pukul ${latestRecord.clockIn}"
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
            ClockSection(navController, attendanceStatus.value, isClockedIn, isClockedOut)
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
    fileUriString: String?,
    onConfirm: (String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                        .addOnSuccessListener { location: Location? ->
                            currentLocation = location
                            locationFetchStatus = if (location != null) "Lokasi ditemukan!" else "Gagal mendapatkan lokasi."
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
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location: Location? ->
                    currentLocation = location
                    locationFetchStatus = if (location != null) "Lokasi ditemukan!" else "Gagal mendapatkan lokasi."
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
                    if (currentLocation == null) {
                        Toast.makeText(context, "Gagal mendapatkan lokasi. Pastikan GPS aktif.", Toast.LENGTH_LONG).show()
                        isSubmitting = false
                        return@Button
                    }

                    val sharedPreferences = context.getSharedPreferences("SITEKAD_PREFS", Context.MODE_PRIVATE)
                    val token = "Bearer ${sharedPreferences.getString("jwt_token", "") ?: ""}"

                    if (attendanceType == "in-lembur") {
                        val fileUri = fileUriString?.toUri()
                        if (fileUri == null) {
                            Toast.makeText(context, "File SPL tidak ditemukan. Silakan pilih ulang.", Toast.LENGTH_LONG).show()
                            isSubmitting = false
                            return@Button
                        }

                        coroutineScope.launch {
                            try {
                                val latitudeRequestBody = currentLocation!!.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                val longitudeRequestBody = currentLocation!!.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                val androidIdRequestBody = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).toRequestBody("text/plain".toMediaTypeOrNull())
                                // --- TAMBAHKAN KODE QR DI SINI ---
                                val qrCodeRequestBody = qrCodeId.toRequestBody("text/plain".toMediaTypeOrNull())


                                val inputStream = context.contentResolver.openInputStream(fileUri)
                                val fileRequestBody = inputStream?.readBytes()?.toRequestBody("image/*".toMediaTypeOrNull())
                                inputStream?.close()

                                if (fileRequestBody == null) {
                                    Toast.makeText(context, "Gagal membaca file SPL.", Toast.LENGTH_LONG).show()
                                    isSubmitting = false
                                    return@launch
                                }

                                val splFilePart = MultipartBody.Part.createFormData("spl_file", getFileName(context, fileUri) ?: "spl.jpg", fileRequestBody)

                                // Memanggil fungsi API yang sudah diupdate dengan parameter kodeqr
                                val response = RetrofitClient.instance.startLembur(token, splFilePart, latitudeRequestBody, longitudeRequestBody, androidIdRequestBody, qrCodeRequestBody)

                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Sukses: ${response.body()}", Toast.LENGTH_SHORT).show()
                                    onConfirm(timeNow, qrCodeId)
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    Log.e("RETROFIT_ERROR", "Error: $errorBody, Code: ${response.code()}")
                                    Toast.makeText(context, "Gagal: $errorBody", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Log.e("RETROFIT_EXCEPTION", "Exception: ${e.message}")
                                Toast.makeText(context, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSubmitting = false
                            }
                        }

                    } else {
                        val url = if (attendanceType == "out-lembur") "http://202.138.248.93:10084/api/lembur/end" else "http://202.138.248.93:10084/api/absensi"
                        val requestQueue = Volley.newRequestQueue(context)
                        val stringRequest = object : StringRequest(Method.POST, url,
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
                                headers["Authorization"] = token
                                return headers
                            }
                            override fun getBodyContentType() = "application/json; charset=utf-8"
                            override fun getBody(): ByteArray {
                                val jsonBody = JSONObject()
                                jsonBody.put("latitude", currentLocation!!.latitude)
                                jsonBody.put("longitude", currentLocation!!.longitude)
                                jsonBody.put("android_id", Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID))
                                jsonBody.put("kodeqr", qrCodeId) // Selalu tambahkan kodeqr
                                return jsonBody.toString().toByteArray(Charsets.UTF_8)
                            }
                        }
                        requestQueue.add(stringRequest)
                    }
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
    fileUriString: String?,
    onFileSelected: (Uri) -> Unit,
    onLogoutClick: () -> Unit,
    onClearFile: () -> Unit
) {
    val fileUri = fileUriString?.toUri()
    var showImagePreviewDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> uri?.let { onFileSelected(it) } }
    )

    if (showImagePreviewDialog && fileUri != null) {
        Dialog(onDismissRequest = { showImagePreviewDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                AsyncImage(model = fileUri, contentDescription = "Full Screen Preview", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
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
            Text("1. Pilih Gambar SPL", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            FilePickerBox(
                fileUri = fileUri,
                onClick = {
                    if (fileUri == null) imagePickerLauncher.launch("image/*")
                    else showImagePreviewDialog = true
                },
                onClearImage = onClearFile
            )
        }

        if (fileUriString != null) {
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
                    Icon(imageVector = Icons.Filled.History, "History Icon", tint = MaterialTheme.colorScheme.onBackground)
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
            .clickable(onClick = onClick)
    ) {
        if (fileUri == null || fileUri.toString().isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = "Upload Icon", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Klik untuk memilih gambar SPL", color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
            }
        } else {
            AsyncImage(
                model = fileUri,
                contentDescription = "Preview SPL",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onClearImage,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Hapus Gambar", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var fileName: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
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
                text = userProfile?.namaLengkap ?: "Memuat...",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
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

@Composable
fun ProfileScreen(userProfile: UserProfile?, onLogoutClick: () -> Unit) {
    var isLoggingOut by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (userProfile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileInfoItem(icon = Icons.Filled.Work, label = "Jabatan", value = userProfile.jabatan)
                    ProfileInfoItem(icon = Icons.Filled.Business, label = "Cabang", value = userProfile.cabang)
                    ProfileInfoItem(icon = Icons.Filled.LocationOn, label = "Lokasi", value = userProfile.lokasi)
                }
                Spacer(modifier = Modifier.weight(1f))
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