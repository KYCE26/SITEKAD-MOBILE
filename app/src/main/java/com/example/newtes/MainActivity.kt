package com.example.newtes // Sesuaikan dengan package name Anda

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.newtes.ui.theme.NewTesTheme

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
                SitekadLandingScreen()
            }
        }
    }
}

@Composable
fun SitekadLandingScreen() {
    var authState by remember { mutableStateOf(AuthState.WELCOME) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bagian atas (Logo dan Judul)
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
                Text(text = "SITEKAD", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Sistem Informasi Tenaga Kerja Ahli Daya", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            }

            // Bagian bawah dinamis
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

@Composable
fun LoginForm(onBackClick: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val correctUsername = "user"
    val correctPassword = "password123"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = sitekadTextFieldColors())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = sitekadTextFieldColors())
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (username == correctUsername && password == correctPassword) {
                    Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, HomeActivity::class.java)
                    intent.putExtra("EXTRA_USERNAME", username)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Username atau Password Salah!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(text = "MASUK", fontWeight = FontWeight.Bold) }
        TextButton(onClick = { Log.d("LoginForm", "Tombol Lupa Password diklik!") }, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = "Lupa Password?", color = MaterialTheme.colorScheme.secondary)
        }
        TextButton(onClick = onBackClick) { Text(text = "Kembali", color = MaterialTheme.colorScheme.secondary) }
    }
}

@Composable
fun RegisterForm(onBackClick: () -> Unit) {
    var fullname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(value = fullname, onValueChange = { fullname = it }, label = { Text("NITAD") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = sitekadTextFieldColors())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = sitekadTextFieldColors())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = sitekadTextFieldColors())
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { /* Logika registrasi nanti di sini */ }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text(text = "DAFTAR", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBackClick) { Text(text = "Kembali", color = MaterialTheme.colorScheme.secondary) }
    }
}

@Preview(showBackground = true)
@Composable
fun SitekadLandingScreenPreview() {
    NewTesTheme {
        SitekadLandingScreen()
    }
}