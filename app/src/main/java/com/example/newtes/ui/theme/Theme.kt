package com.example.newtes.ui.theme // Sesuaikan dengan package name Anda

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Definisikan skema warna gelap menggunakan palet warna SITEKAD
private val SitekadColorScheme = darkColorScheme(
    primary = BrandRed,
    secondary = MediumGray,
    background = Charcoal,
    surface = DarkGray,
    onPrimary = OffWhite,   // Warna teks di atas tombol utama (Merah)
    onSecondary = OffWhite,
    onBackground = OffWhite, // Warna teks utama di atas background
    onSurface = OffWhite
)

@Composable
fun NewTesTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SitekadColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}