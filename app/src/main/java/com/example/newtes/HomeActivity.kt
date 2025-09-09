package com.example.newtes // Sesuaikan dengan package name Anda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector // <-- IMPORT PENTING YANG KURANG
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.newtes.ui.theme.NewTesTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import android.app.Activity

// Data Class untuk menampung data riwayat absensi
data class AttendanceRecord(
    val date: String,
    val day: String,
    val clockIn: String,
    val clockOut: String,
    val isLate: Boolean = false
)

// Rute untuk Navigasi Bawah
sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
    object Home : BottomNavItem("home_main", Icons.Default.Home, "Home")
    object Lembur : BottomNavItem("lembur", Icons.Default.Notifications, "Lembur")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
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

// Wrapper untuk Scaffold dan Navigasi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(username: String) {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Lembur,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 5.dp
            ) {
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
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
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
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreenContent(username = username)
            }
            composable(BottomNavItem.Lembur.route) {
                PlaceholderScreen(text = "Halaman Permintaan Lembur")
            }
            composable(BottomNavItem.Profile.route) {
                PlaceholderScreen(text = "Halaman Profil Pengguna")
            }
        }
    }
}

// Composable untuk layar placeholder
@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp
        )
    }
}


// Konten Layar Home
@Composable
fun HomeScreenContent(username: String) {
    val dummyHistory = listOf(
        AttendanceRecord("14 April 2023", "Fri", "08:00 AM", "05:00 PM"),
        AttendanceRecord("13 April 2023", "Thu", "08:45 AM", "05:00 PM", isLate = true),
        AttendanceRecord("12 April 2023", "Wed", "07:55 AM", "05:00 PM"),
        AttendanceRecord("11 April 2023", "Tue", "07:58 AM", "05:00 PM"),
        AttendanceRecord("10 April 2023", "Mon", "08:15 AM", "05:00 PM", isLate = true),
        AttendanceRecord("14 April 2023", "Fri", "08:00 AM", "05:00 PM"),
        AttendanceRecord("13 April 2023", "Thu", "08:45 AM", "05:00 PM", isLate = true),
        AttendanceRecord("12 April 2023", "Wed", "07:55 AM", "05:00 PM"),
        AttendanceRecord("11 April 2023", "Tue", "07:58 AM", "05:00 PM"),
        AttendanceRecord("10 April 2023", "Mon", "08:15 AM", "05:00 PM", isLate = true)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            UserHeader(username = username)
            Spacer(modifier = Modifier.height(32.dp))
        }
        item {
            ClockSection()
            Spacer(modifier = Modifier.height(32.dp))
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = "History Icon", tint = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attendance History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(dummyHistory) { record ->
            HistoryItem(record = record)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// UserHeader tetap sama
@Composable
fun UserHeader(username: String) {
    // Dapatkan context saat ini
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "User Avatar", modifier = Modifier.size(50.dp).clip(CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = username, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "27738749 - Senior UX Designer", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {
            // --- LOGIKA LOGOUT BARU ---
            // Mengubah context menjadi Activity, lalu memanggil finish() untuk menutupnya.
            (context as? Activity)?.finish()
        }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}
// ClockSection tetap sama
@Composable
fun ClockSection() {
    var currentTime by remember { mutableStateOf("--:--:--") }
    var currentDate by remember { mutableStateOf("") }
    var isClockedIn by remember { mutableStateOf(false) }
    var isClockedOut by remember { mutableStateOf(false) }

    // Menggunakan Locale yang lebih modern untuk menghindari warning
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = currentTime, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = currentDate, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { isClockedIn = true },
                modifier = Modifier.weight(1f).height(50.dp),
                enabled = !isClockedIn,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Clock In", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = { isClockedOut = true },
                modifier = Modifier.weight(1f).height(50.dp),
                enabled = isClockedIn && !isClockedOut,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) { Text("Clock Out", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}

// HistoryItem tetap sama
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