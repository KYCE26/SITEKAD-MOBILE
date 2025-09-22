package com.example.newtes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {

    // --- State Aplikasi ---
    private val _userProfile = mutableStateOf<UserProfile?>(null)
    val userProfile: State<UserProfile?> = _userProfile

    // State Absen Biasa
    private val _attendanceStatus = mutableStateOf("Memuat...")
    val attendanceStatus: State<String> = _attendanceStatus
    private val _isClockedIn = mutableStateOf(false)
    val isClockedIn: State<Boolean> = _isClockedIn
    private val _isClockedOut = mutableStateOf(false)
    val isClockedOut: State<Boolean> = _isClockedOut
    private val _attendanceHistory = mutableStateListOf<AttendanceRecord>()
    val attendanceHistory: List<AttendanceRecord> = _attendanceHistory

    // State Lembur
    private val _lemburStatus = mutableStateOf("Memuat...")
    val lemburStatus: State<String> = _lemburStatus
    private val _isLemburClockedIn = mutableStateOf(false)
    val isLemburClockedIn: State<Boolean> = _isLemburClockedIn
    private val _isLemburClockedOut = mutableStateOf(false)
    val isLemburClockedOut: State<Boolean> = _isLemburClockedOut
    private val _lemburHistory = mutableStateListOf<AttendanceRecord>()
    val lemburHistory: List<AttendanceRecord> = _lemburHistory
    private val _fileUriString = mutableStateOf<String?>(null)
    val fileUriString: State<String?> = _fileUriString
    private val _hasStaleOvertime = mutableStateOf(false)
    val hasStaleOvertime: State<Boolean> = _hasStaleOvertime


    // --- Logika & Aksi User ---

    fun onFileSelected(uri: Uri?) {
        _fileUriString.value = uri?.toString()
    }

    fun onClearFile() {
        _fileUriString.value = null
    }

    fun doLogout(context: Context) {
        val sharedPreferences = context.getSharedPreferences("SITEKAD_PREFS", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }

    fun fetchData(context: Context) {
        val requestQueue = Volley.newRequestQueue(context)
        val sharedPreferences = context.getSharedPreferences("SITEKAD_PREFS", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", null)

        fetchUserProfile(requestQueue, token)
        fetchAttendanceHistory(requestQueue, token)
        fetchLemburHistory(requestQueue, token)
    }

    private fun fetchUserProfile(queue: com.android.volley.RequestQueue, token: String?) {
        val url = "http://202.138.248.93:10084/api/profile"
        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val profileJson = response.getJSONObject("profile")
                    _userProfile.value = UserProfile(
                        username = profileJson.getString("Username"),
                        nitad = profileJson.getString("NITAD"),
                        namaLengkap = profileJson.getString("Nama Lengkap"),
                        jabatan = profileJson.getString("Jabatan"),
                        cabang = profileJson.getString("Cabang"),
                        lokasi = profileJson.getString("Lokasi")
                    )
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error parsing profile: ${e.message}")
                }
            },
            { error -> Log.e("MainViewModel", "Error fetching profile: ${error.message}") }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        queue.add(request)
    }

    private fun fetchAttendanceHistory(queue: com.android.volley.RequestQueue, token: String?) {
        val url = "http://202.138.248.93:10084/api/uhistori"
        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val historyArray = response.getJSONArray("history")
                    val records = mutableListOf<AttendanceRecord>()
                    for (i in 0 until historyArray.length()) {
                        val item = historyArray.getJSONObject(i)
                        val (day, date) = formatDate(item.getString("tgl_absen"))
                        records.add(
                            AttendanceRecord(
                                date = date,
                                day = day,
                                clockIn = item.getString("jam_masuk"),
                                clockOut = if (item.isNull("jam_keluar")) "--:--:--" else item.getString("jam_keluar"),
                                isLate = false
                            )
                        )
                    }
                    _attendanceHistory.clear()
                    _attendanceHistory.addAll(records)

                    // --- LOGIKA BARU UNTUK MENDUKUNG SHIFT MALAM (ABSEN BIASA) ---
                    val openSession = _attendanceHistory.firstOrNull { it.clockOut == "--:--:--" }

                    if (openSession != null) {
                        _attendanceStatus.value = "Hadir - Masuk pukul ${openSession.clockIn} (${openSession.day}, ${openSession.date})"
                        _isClockedIn.value = true
                        _isClockedOut.value = false
                    } else {
                        _attendanceStatus.value = "Belum Absen Hari Ini"
                        _isClockedIn.value = false
                        _isClockedOut.value = false
                    }

                } catch (e: Exception) { Log.e("ViewModel", "Parse Error Uhistori: ${e.message}") }
            },
            { error -> Log.e("ViewModel", "API Error Uhistori: ${error.message}") }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        queue.add(request)
    }

    private fun fetchLemburHistory(queue: com.android.volley.RequestQueue, token: String?) {
        val url = "http://202.138.248.93:10084/api/lembur/history"
        val request = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val historyArray = response.getJSONArray("history")
                    val records = mutableListOf<AttendanceRecord>()
                    for (i in 0 until historyArray.length()) {
                        val item = historyArray.getJSONObject(i)
                        val (day, date) = formatDate(item.getString("tgl_absen"))
                        records.add(
                            AttendanceRecord(
                                date = date,
                                day = day,
                                clockIn = item.getString("jam_masuk"),
                                clockOut = if (item.isNull("jam_keluar")) "--:--:--" else item.getString("jam_keluar"),
                                isLate = false
                            )
                        )
                    }
                    _lemburHistory.clear()
                    _lemburHistory.addAll(records)

                    // --- LOGIKA BARU UNTUK MENDUKUNG SHIFT MALAM (LEMBUR) ---
                    val openLemburSession = _lemburHistory.firstOrNull { it.clockOut == "--:--:--" }

                    if (openLemburSession != null) {
                        _lemburStatus.value = "Lembur - Masuk pukul ${openLemburSession.clockIn}"
                        _isLemburClockedIn.value = true
                        _isLemburClockedOut.value = false
                        _hasStaleOvertime.value = false
                    } else {
                        _lemburStatus.value = "Belum Lembur Hari Ini"
                        _isLemburClockedIn.value = false
                        _isLemburClockedOut.value = false
                        _hasStaleOvertime.value = false
                    }

                } catch (e: Exception) { Log.e("ViewModel", "Parse Error Lembur History: ${e.message}") }
            },
            { error -> Log.e("ViewModel", "API Error Lembur History: ${error.message}") }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }
        queue.add(request)
    }

    private fun formatDate(dateString: String): Pair<String, String> {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormatDate = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val outputFormatDay = SimpleDateFormat("EEEE", Locale("id", "ID"))
        return try {
            val date = inputFormat.parse(dateString)
            Pair(outputFormatDay.format(date!!), outputFormatDate.format(date))
        } catch (e: Exception) {
            Pair("", "")
        }
    }
}