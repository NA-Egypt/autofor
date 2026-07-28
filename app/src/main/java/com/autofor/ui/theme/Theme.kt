package com.autofor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4D8EFF),
    secondary = Color(0xFF00C853),
    tertiary = Color(0xFF00B0FF),
    background = Color(0xFF121418),
    surface = Color(0xFF1E222A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1967D2),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFF0288D1),
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun AutoForTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
