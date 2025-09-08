package com.example.newtes // PASTIKAN package name ini sesuai dengan proyek Anda

// GANTI SEMUA IMPORT ANDA DENGAN BLOK DI BAWAH INI
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newtes.ui.theme.NewTesTheme
import com.example.newtes.ui.theme.NavyBlue
import com.example.newtes.ui.theme.SkyBlue

// 1. KITA BUAT "STATE" UNTUK MENENTUKAN TAMPILAN
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
                SitekadLandingScreen()
            }
        }
    }
}

@Composable
fun SitekadLandingScreen() {
    // State ini akan mengingat mode mana yang sedang aktif
    var authState by remember { mutableStateOf(AuthState.WELCOME) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NavyBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bagian atas (Logo dan Judul) tetap sama
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo SITEKAD",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "SITEKAD",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sistem Informasi Tenaga Kerja Ahli Daya",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }

            // 2. BAGIAN BAWAH INI AKAN BERUBAH-UBAH SESUAI STATE
            AnimatedContent(
                targetState = authState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "Auth Form Animation"
            ) { state ->
                when (state) {
                    AuthState.WELCOME -> WelcomeButtons(
                        onLoginClick = { authState = AuthState.LOGIN },
                        onRegisterClick = { authState = AuthState.REGISTER }
                    )
                    AuthState.LOGIN -> LoginForm(
                        onBackClick = { authState = AuthState.WELCOME }
                    )
                    AuthState.REGISTER -> RegisterForm(
                        onBackClick = { authState = AuthState.WELCOME }
                    )
                }
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
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)
        ) { Text(text = "LOGIN", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SkyBlue)
        ) { Text(text = "REGISTRASI", color = SkyBlue, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun LoginForm(onBackClick: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = SkyBlue,
                focusedLabelColor = SkyBlue,
                unfocusedLabelColor = Color.Gray,
                focusedIndicatorColor = SkyBlue,
                unfocusedIndicatorColor = Color.Gray
            )
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
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = SkyBlue,
                focusedLabelColor = SkyBlue,
                unfocusedLabelColor = Color.Gray,
                focusedIndicatorColor = SkyBlue,
                unfocusedIndicatorColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* Logika login nanti di sini */ },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)
        ) { Text(text = "MASUK", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackClick) {
            Text(text = "Kembali", color = SkyBlue)
        }
    }
}

@Composable
fun RegisterForm(onBackClick: () -> Unit) {
    // Untuk contoh, kita buat sama dengan login, bisa ditambahkan field lain nanti
    var fullname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(value = fullname, onValueChange = { fullname = it }, label = { Text("NITAD") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White,unfocusedTextColor = Color.White,focusedContainerColor = Color.Transparent,unfocusedContainerColor = Color.Transparent,cursorColor = SkyBlue,focusedLabelColor = SkyBlue,unfocusedLabelColor = Color.Gray,focusedIndicatorColor = SkyBlue,unfocusedIndicatorColor = Color.Gray))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedTextColor = Color.White,unfocusedTextColor = Color.White,focusedContainerColor = Color.Transparent,unfocusedContainerColor = Color.Transparent,cursorColor = SkyBlue,focusedLabelColor = SkyBlue,unfocusedLabelColor = Color.Gray,focusedIndicatorColor = SkyBlue,unfocusedIndicatorColor = Color.Gray))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = TextFieldDefaults.colors(focusedTextColor = Color.White,unfocusedTextColor = Color.White,focusedContainerColor = Color.Transparent,unfocusedContainerColor = Color.Transparent,cursorColor = SkyBlue,focusedLabelColor = SkyBlue,unfocusedLabelColor = Color.Gray,focusedIndicatorColor = SkyBlue,unfocusedIndicatorColor = Color.Gray))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { /* Logika registrasi nanti di sini */ }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)) { Text(text = "DAFTAR", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackClick) { Text(text = "Kembali", color = SkyBlue) }
    }
}