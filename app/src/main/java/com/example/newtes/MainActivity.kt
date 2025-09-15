package com.example.newtes // Sesuaikan dengan package name Anda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // <-- Diganti dari Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.newtes.ui.theme.NewTesTheme
import org.json.JSONObject
// Enum untuk menentukan state tampilan
enum class AuthState {
    WELCOME,
    LOGIN,
    REGISTER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewTesTheme {
                SitekadAuthScreen()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SitekadAuthScreen() {
    var authState by remember { mutableStateOf(AuthState.WELCOME) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // --- PERUBAHAN UTAMA: MENGGUNAKAN LazyColumn ---
        // LazyColumn secara alami mendukung scroll, yang akan aktif saat keyboard muncul
        // dan mendorong konten ke atas.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Bagian atas (Logo dan Judul)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillParentMaxHeight(0.5f) // Mengisi 50% ruang atas
                        .padding(top = 48.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo SITEKAD", modifier = Modifier.size(120.dp), contentScale = ContentScale.Fit)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "SITEKAD", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Sistem Informasi Tenaga Kerja Ahli Daya", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                }
            }

            // Bagian bawah dinamis
            item {
                AnimatedContent(
                    targetState = authState,
                    transitionSpec = { fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300)) },
                    label = "Auth Form Animation"
                ) { state ->
                    when (state) {
                        AuthState.WELCOME -> WelcomeButtons(
                            onLoginClick = { authState = AuthState.LOGIN },
                            onRegisterClick = { authState = AuthState.REGISTER }
                        )
                        AuthState.LOGIN -> LoginForm(onBackClick = { authState = AuthState.WELCOME })
                        AuthState.REGISTER -> RegisterForm(
                            onBackClick = { authState = AuthState.WELCOME },
                            onRegisterSuccess = { authState = AuthState.LOGIN }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun WelcomeButtons(onLoginClick: () -> Unit, onRegisterClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(text = "LOGIN", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        ) { Text(text = "REGISTRASI", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun sitekadTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary
)

// Di dalam file MainActivity.kt

@Composable
fun LoginForm(onBackClick: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val requestQueue = remember { Volley.newRequestQueue(context) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = sitekadTextFieldColors()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = sitekadTextFieldColors()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                isLoading = true
                // GANTI DENGAN IP ANDA & PORT LOGIN
                val url = "http://202.138.248.93:10084/login"

                val stringRequest = object : StringRequest(
                    Method.POST, url,
                    { response ->
                        isLoading = false

                        try {
                            // 1. "Bongkar" String JSON menjadi sebuah objek
                            val jsonObject = JSONObject(response)

                            // 2. Ambil HANYA nilai dari kunci "token"
                            val jwtToken = jsonObject.getString("token")

                            Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()

                            // 3. Simpan HANYA tokennya yang sudah bersih
                            val sharedPreferences = context.getSharedPreferences("SITEKAD_PREFS", Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putString("jwt_token", jwtToken)
                                apply()
                            }

                            // 4. Pindah ke Halaman Home
                            val intent = Intent(context, HomeActivity::class.java)
                            intent.putExtra("EXTRA_USERNAME", username)
                            context.startActivity(intent)

                        } catch (e: Exception) {
                            // Jika format JSON dari server tidak sesuai dugaan
                            isLoading = false
                            Toast.makeText(context, "Gagal memproses data dari server", Toast.LENGTH_LONG).show()
                            Log.e("LOGIN_API", "JSON Parsing Error: ${e.message}")
                        }
                    },
                    { error -> // Jika GAGAL
                        isLoading = false
                        val errorMessage = error.networkResponse?.let {
                            String(it.data, Charsets.UTF_8)
                        } ?: "Login Gagal: ${error.message}"

                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e("LOGIN_API", "Error: ${error.toString()}")
                    }) {

                    override fun getBodyContentType(): String {
                        return "application/json; charset=utf-8"
                    }

                    override fun getBody(): ByteArray {
                        val jsonBody = JSONObject()
                        jsonBody.put("username", username)
                        jsonBody.put("password", password)
                        return jsonBody.toString().toByteArray(Charsets.UTF_8)
                    }
                }

                requestQueue.add(stringRequest)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(text = "MASUK", fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = { Log.d("LoginForm", "Tombol Lupa Password diklik!") }, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = "Lupa Password?", color = MaterialTheme.colorScheme.secondary)
        }
        TextButton(onClick = onBackClick) { Text(text = "Kembali", color = MaterialTheme.colorScheme.secondary) }
    }
}
@Composable
fun RegisterForm(onBackClick: () -> Unit, onRegisterSuccess: () -> Unit) {
    var fullname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val requestQueue = remember { Volley.newRequestQueue(context) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(value = fullname, onValueChange = { fullname = it }, label = { Text("NITAD") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = sitekadTextFieldColors())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = sitekadTextFieldColors())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = sitekadTextFieldColors())
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                isLoading = true
                val url = "http://202.138.248.93:10084/aktivasi"

                val stringRequest = object: StringRequest(
                    Method.POST, url,
                    { response ->
                        isLoading = false
                        Toast.makeText(context, "Registrasi Berhasil! Silakan Login.", Toast.LENGTH_LONG).show()
                        onRegisterSuccess()
                    },
                    { error ->
                        isLoading = false
                        val errorMessage = error.networkResponse?.let { String(it.data) } ?: "Registrasi Gagal: ${error.message}"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e("REGISTER_API", "Error: ${error.toString()}")
                    }) {
                    override fun getBodyContentType(): String {
                        return "application/json; charset=utf-8"
                    }

                    override fun getBody(): ByteArray {
                        val jsonBody = JSONObject()
                        jsonBody.put("NITAD", fullname)
                        jsonBody.put("username", username)
                        jsonBody.put("password", password)
                        return jsonBody.toString().toByteArray(Charsets.UTF_8)
                    }
                }
                requestQueue.add(stringRequest)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(text = "DAFTAR", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackClick) { Text(text = "Kembali", color = MaterialTheme.colorScheme.secondary) }
    }
}

@Preview(showBackground = true)
@Composable
fun SitekadAuthScreenPreview() {
    NewTesTheme {
        SitekadAuthScreen()
    }
}