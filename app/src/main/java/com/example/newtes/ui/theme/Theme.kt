package com.example.newtes.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // <-- TAMBAHKAN BARIS INI
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


// Definisikan skema warna gelap menggunakan warna kustom SITEKAD
private val SitekadColorScheme = darkColorScheme(
    primary = SkyBlue,
    secondary = SteelBlue,
    background = NavyBlue,
    surface = DarkBlue,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = OffWhite,
    onSurface = OffWhite
)

@Composable
fun NewTesTheme(
    // Aplikasi SITEKAD kita desain untuk tema gelap, jadi tidak perlu parameter light/dark
    content: @Composable () -> Unit
) {
    val colorScheme = SitekadColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Kita atur warna status bar menjadi warna background utama
            window.statusBarColor = colorScheme.background.toArgb()
            // Ikon di status bar (baterai, jam) akan berwarna terang
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}