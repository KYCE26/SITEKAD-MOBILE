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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
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
    // Tambahkan object baru di sini
    object PengajuanCuti : Screen("pengajuan_cuti")
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
fun MainAppScreen(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Ambil semua state dari ViewModel
    val userProfile by viewModel.userProfile
    val attendanceStatus by viewModel.attendanceStatus
    val isClockedIn by viewModel.isClockedIn
    val isClockedOut by viewModel.isClockedOut
    val attendanceHistory = viewModel.attendanceHistory
    val lemburStatus by viewModel.lemburStatus
    val isLemburClockedIn by viewModel.isLemburClockedIn
    val isLemburClockedOut by viewModel.isLemburClockedOut
    val lemburHistory = viewModel.lemburHistory
    val fileUriString by viewModel.fileUriString

    val bottomNavItems = listOf(Screen.Home, Screen.Lembur, Screen.Profile)

    // Auto-refresh dan refresh saat aplikasi kembali dibuka (on resume)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchData(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Polling setiap 30 detik
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000L)
            viewModel.fetchData(context)
        }
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
                                else -> {} // <-- TAMBAHKAN INI
                            }
                        },
                        label = {
                            when (item) {
                                is Screen.Home -> Text("Home")
                                is Screen.Lembur -> Text("Lembur")
                                is Screen.Profile -> Text("Profile")
                                else -> {} // <-- TAMBAHKAN INI JUGA
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
                    userProfile = userProfile,
                    navController = navController,
                    attendanceStatus = attendanceStatus,
                    isClockedIn = isClockedIn,
                    isClockedOut = isClockedOut,
                    history = attendanceHistory.toMutableList(),
                    onLogoutClick = { viewModel.doLogout(context) }
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
                    fileUriString = fileUriString,
                    onConfirm = { _, _ ->
                        viewModel.fetchData(context) // Refresh data setelah aksi
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Lembur.route) {
                LemburScreen(
                    userProfile = userProfile,
                    navController = navController,
                    lemburStatus = lemburStatus,
                    isLemburClockedIn = isLemburClockedIn,
                    isLemburClockedOut = isLemburClockedOut,
                    history = lemburHistory.toMutableList(),
                    fileUriString = fileUriString,
                    onFileSelected = viewModel::onFileSelected,
                    onLogoutClick = { viewModel.doLogout(context) },
                    onClearFile = viewModel::onClearFile
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(userProfile = userProfile, onLogoutClick = { viewModel.doLogout(context) })
            }
            composable(Screen.PengajuanCuti.route) {
                PengajuanCutiScreen(navController = navController)
            }
        }
    }
}

@Composable
fun MainScreenBackground(content: @Composable () -> Unit) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background
        )
    )
    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundBrush)) {
        content()
    }
}

@Composable
fun HomeScreenContent(
    userProfile: UserProfile?,
    navController: NavHostController,
    attendanceStatus: String,
    isClockedIn: Boolean,
    isClockedOut: Boolean,
    history: MutableList<AttendanceRecord>,
    onLogoutClick: () -> Unit
) {
    MainScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                UserHeader(userProfile = userProfile, onLogoutClick = onLogoutClick)
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                ClockSection(navController, attendanceStatus, isClockedIn, isClockedOut, isLembur = false)
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                CutiCard(onClick = { navController.navigate(Screen.PengajuanCuti.route) })
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.History, contentDescription = "History Icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Riwayat Kehadiran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Memuat riwayat...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            } else {
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = currentTime, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = currentDate, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Status Kehadiran Anda:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
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
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if(isLembur) "Clock Out Lembur" else "Clock Out", fontWeight = FontWeight.Bold) }
            }
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

                                val response = RetrofitClient.instance.startLembur(token, splFilePart, latitudeRequestBody, longitudeRequestBody, androidIdRequestBody, qrCodeRequestBody)

                                if (response.isSuccessful) {
                                    val successMessage = response.body()?.message ?: "Lembur berhasil dimulai"
                                    Toast.makeText(context, "Sukses: $successMessage", Toast.LENGTH_SHORT).show()
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
                        val url = if (attendanceType == "out-lembur") "http://202.138.248.93:11084/v1/api/lembur/end" else "http://202.138.248.93:11084/v1/api/absensi"
                        val method = if (attendanceType == "out-lembur") Request.Method.PUT else Request.Method.POST
                        val requestQueue = Volley.newRequestQueue(context)
                        val stringRequest = object : StringRequest(method, url,
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
                                if (attendanceType != "out-lembur") {
                                    jsonBody.put("kodeqr", qrCodeId)
                                }
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
    history: MutableList<AttendanceRecord>,
    fileUriString: String?,
    onFileSelected: (Uri?) -> Unit,
    onLogoutClick: () -> Unit,
    onClearFile: () -> Unit
) {
    val fileUri = fileUriString?.toUri()
    var showImagePreviewDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> onFileSelected(uri) }
    )

    if (showImagePreviewDialog && fileUri != null) {
        Dialog(onDismissRequest = { showImagePreviewDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                AsyncImage(model = fileUri, contentDescription = "Full Screen Preview", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
            }
        }
    }

    MainScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                UserHeader(userProfile = userProfile, onLogoutClick = onLogoutClick)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text("Pengajuan Lembur", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (!isLemburClockedOut) {
                item {
                    Text("1. Unggah Surat Perintah Lembur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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

                if (fileUriString != null || isLemburClockedIn) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("2. Lakukan Absensi Lembur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        ClockSection(
                            navController = navController,
                            attendanceStatus = lemburStatus,
                            isClockedIn = isLemburClockedIn,
                            isClockedOut = isLemburClockedOut,
                            isLembur = true
                        )
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Text(
                            text = "Anda sudah menyelesaikan sesi lembur hari ini.",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.History, "History Icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Riwayat Lembur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Memuat riwayat lembur...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            } else {
                items(history) { record ->
                    HistoryItem(record = record)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(userProfile: UserProfile?, onLogoutClick: () -> Unit) {
    MainScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userProfile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = userProfile.namaLengkap, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = "NITAD: ${userProfile.nitad}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        ProfileInfoItem(icon = Icons.Filled.Work, label = "Jabatan", value = userProfile.jabatan)
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoItem(icon = Icons.Filled.Business, label = "Cabang", value = userProfile.cabang)
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileInfoItem(icon = Icons.Filled.LocationOn, label = "Lokasi", value = userProfile.lokasi)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("LOGOUT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun FilePickerBox(
    fileUri: Uri?,
    onClick: () -> Unit,
    onClearImage: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable(onClick = onClick)
        ) {
            if (fileUri == null || fileUri.toString().isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = "Upload Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Klik untuk memilih gambar SPL", color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
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
fun UserHeader(userProfile: UserProfile?, onLogoutClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = userProfile?.namaLengkap ?: "Memuat...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = userProfile?.nitad ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onLogoutClick) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun HistoryItem(record: AttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (record.isLate) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.isLate) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = "Status",
                    tint = if (record.isLate) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${record.day}, ${record.date}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Masuk: ${record.clockIn} - Keluar: ${record.clockOut}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutiCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.EventAvailable,
                contentDescription = "Cuti Icon",
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pengajuan Cuti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ajukan cuti dengan melampirkan surat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Go to Cuti",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengajuanCutiScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- State untuk field-field baru ---
    var alasan by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var isAlasanExpanded by remember { mutableStateOf(false) }
    val daftarAlasan = listOf("Sakit", "Cuti Tahunan", "Keperluan Mendesak", "Lainnya")

    var isLoading by remember { mutableStateOf(false) }

    // Launcher untuk memilih gambar
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> fileUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengajuan Cuti") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Kita gunakan LazyColumn agar bisa di-scroll jika field-nya banyak
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Field Alasan (Dropdown) ---
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = isAlasanExpanded,
                    onExpandedChange = { isAlasanExpanded = !isAlasanExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = alasan,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Alasan Cuti *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAlasanExpanded)
                        },
                        colors = sitekadTextFieldColors(), // Pakai warna konsisten
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isAlasanExpanded,
                        onDismissRequest = { isAlasanExpanded = false }
                    ) {
                        daftarAlasan.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    alasan = item
                                    isAlasanExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- Field Deskripsi (Opsional) ---
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = deskripsi,
                    onValueChange = { deskripsi = it },
                    label = { Text("Deskripsi (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = sitekadTextFieldColors(),
                    minLines = 3 // Bikin lebih tinggi
                )
            }

            // --- Field Foto (Opsional) ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Foto Pendukung (Opsional)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Menggunakan kembali FilePickerBox
                FilePickerBox(
                    fileUri = fileUri,
                    onClick = { imagePickerLauncher.launch("image/*") },
                    onClearImage = { fileUri = null }
                )
            }

            // --- Tombol Kirim ---
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        // Validasi HANYA pada 'alasan'
                        if (alasan.isEmpty()) {
                            Toast.makeText(context, "Harap pilih alasan cuti terlebih dahulu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true

                        // --- Logika Mock Diperbarui ---
                        coroutineScope.launch {
                            delay(2000) // delay 2 detik

                            // Siapkan pesan log untuk Toast
                            val waktuSekarang = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                            val fotoStatus = if (fileUri != null) "Ada" else "Tidak Ada"
                            val logMessage = "Sukses!\nAlasan: $alasan\nWaktu: $waktuSekarang\nFoto: $fotoStatus"

                            isLoading = false
                            Toast.makeText(context, logMessage, Toast.LENGTH_LONG).show()
                            navController.popBackStack()
                        }
                        // --- Logika Mock Selesai ---
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("AJUKAN CUTI", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}