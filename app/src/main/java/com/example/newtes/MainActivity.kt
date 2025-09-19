package com.example.newtes // Sesuaikan dengan package name Anda

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import java.nio.charset.StandardCharsets

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

    // Gradasi untuk latar belakang
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround // Memberi ruang atas dan bawah
        ) {
            // Bagian atas (Logo dan Judul)
            item {
                Spacer(modifier = Modifier.height(64.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo SITEKAD",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "SITEKAD",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sistem Informasi Tenaga Kerja Ahli Daya",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bagian bawah dinamis
            item {
                AnimatedContent(
                    targetState = authState,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            // Masuk dari kanan, keluar ke kiri
                            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) togetherWith
                                    slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400))
                        } else {
                            // Masuk dari kiri, keluar ke kanan
                            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) togetherWith
                                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400))
                        }
                    },
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) { Text(text = "LOGIN", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(text = "REGISTRASI", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable
fun sitekadTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
)

@Composable
fun LoginForm(onBackClick: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val requestQueue = remember { Volley.newRequestQueue(context) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Selamat Datang Kembali", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username Icon") },
                colors = sitekadTextFieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = sitekadTextFieldColors()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    isLoading = true
                    val url = "http://202.138.248.93:10084/login"

                    val stringRequest = object : StringRequest(
                        Method.POST, url,
                        { response ->
                            isLoading = false
                            try {
                                val jsonObject = JSONObject(response)
                                val jwtToken = jsonObject.getString("token")
                                Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                                val sharedPreferences = context.getSharedPreferences("SITEKAD_PREFS", Context.MODE_PRIVATE)
                                with(sharedPreferences.edit()) {
                                    putString("jwt_token", jwtToken)
                                    apply()
                                }
                                val intent = Intent(context, HomeActivity::class.java)
                                context.startActivity(intent)
                                (context as? Activity)?.finish() // Menutup activity login
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "Gagal memproses data dari server", Toast.LENGTH_LONG).show()
                                Log.e("LOGIN_API", "JSON Parsing Error: ${e.message}")
                            }
                        },
                        { error ->
                            isLoading = false
                            val errorMessage = error.networkResponse?.let {
                                String(it.data, Charsets.UTF_8)
                            } ?: "Login Gagal: ${error.message}"
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            Log.e("LOGIN_API", "Error: ${error.toString()}")
                        }) {
                        override fun getBodyContentType() = "application/json; charset=utf-8"
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
            TextButton(onClick = { /* TODO */ }, modifier = Modifier.padding(top = 8.dp)) {
                Text(text = "Lupa Password?", color = MaterialTheme.colorScheme.secondary)
            }
            TextButton(onClick = onBackClick) { Text(text = "Kembali", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) }
        }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Buat Akun Baru", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

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
                            val errorMessage = error.networkResponse?.let { String(it.data, Charsets.UTF_8) } ?: "Registrasi Gagal: ${error.message}"
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            Log.e("REGISTER_API", "Error: ${error.toString()}")
                        }) {
                        override fun getBodyContentType() = "application/json; charset=utf-8"
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
            TextButton(onClick = onBackClick) { Text(text = "Kembali", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SitekadAuthScreenPreview() {
    NewTesTheme {
        SitekadAuthScreen()
    }
}