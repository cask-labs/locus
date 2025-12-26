package com.locus.android.ui.theme

import androidx.compose.ui.graphics.Color

// Fallback Palette (Static "Locus Blue" - Android < 12)
val LocusBlue = Color(0xFF2196F3)
val LocusBlueDark = Color(0xFF1976D2)
val LocusBlueLight = Color(0xFFBBDEFB)
val LocusRed = Color(0xFFD32F2F)
val LocusGreen = Color(0xFF388E3C)

// Material 3 Light Scheme Container
val LightColorScheme =
    androidx.compose.material3.lightColorScheme(
        primary = LocusBlue,
        onPrimary = Color.White,
        primaryContainer = LocusBlueLight,
        onPrimaryContainer = Color.Black,
        secondary = LocusBlue,
        // Monochromatic fallback
        onSecondary = Color.White,
        error = LocusRed,
        onError = Color.White,
    )

// Material 3 Dark Scheme Container
val DarkColorScheme =
    androidx.compose.material3.darkColorScheme(
        primary = LocusBlue,
        onPrimary = Color.Black,
        primaryContainer = LocusBlueDark,
        onPrimaryContainer = Color.White,
        secondary = LocusBlue,
        // Monochromatic fallback
        onSecondary = Color.Black,
        error = LocusRed,
        onError = Color.Black,
    )
