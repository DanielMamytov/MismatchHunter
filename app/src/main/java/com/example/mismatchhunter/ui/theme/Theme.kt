package com.example.mismatchhunter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MismatchHunterTheme(
    darkTheme: Boolean = true,
    accent: Color = MhPrimaryBlue,
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        background = MhBackground,
        surface = MhSurface,
        onSurface = MhOnSurface,
        primary = accent,
        secondary = MhSecondaryBlue,
        onPrimary = Color.White,
        error = MhError
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
