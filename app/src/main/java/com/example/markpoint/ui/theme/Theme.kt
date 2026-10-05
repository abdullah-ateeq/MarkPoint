package com.example.markpoint.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B66),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE6E2),
    onPrimaryContainer = Color(0xFF0E2B28),
    secondary = Color(0xFF5B6157),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E4DA),
    onSecondaryContainer = Color(0xFF1B1E19),
    tertiary = Color(0xFF8A5A2B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6DEC4),
    onTertiaryContainer = Color(0xFF2E1800),
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF1E2022),
    surface = Color(0xFFF7F5F0),
    onSurface = Color(0xFF1E2022),
    surfaceVariant = Color(0xFFE6E3DC),
    onSurfaceVariant = Color(0xFF5A5C5E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F0EA),
    surfaceContainer = Color(0xFFECEAE4),
    surfaceContainerHigh = Color(0xFFE6E4DE),
    surfaceContainerHighest = Color(0xFFE0DED8),
    outline = Color(0xFF8C8E8F),
    outlineVariant = Color(0xFFD3D1CB),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CC7BE),
    onPrimary = Color(0xFF00332F),
    primaryContainer = Color(0xFF1D4A46),
    onPrimaryContainer = Color(0xFFBDEAE3),
    secondary = Color(0xFFBFC4B8),
    onSecondary = Color(0xFF2B2F28),
    secondaryContainer = Color(0xFF2E322C),
    onSecondaryContainer = Color(0xFFDDE1D5),
    tertiary = Color(0xFFE5BC8E),
    onTertiary = Color(0xFF452B07),
    tertiaryContainer = Color(0xFF5E401C),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFF0F1112),
    onBackground = Color(0xFFE4E2DD),
    surface = Color(0xFF0F1112),
    onSurface = Color(0xFFE4E2DD),
    surfaceVariant = Color(0xFF2A2E30),
    onSurfaceVariant = Color(0xFFA9ADAF),
    surfaceContainerLowest = Color(0xFF0A0C0D),
    surfaceContainerLow = Color(0xFF15181A),
    surfaceContainer = Color(0xFF1A1E20),
    surfaceContainerHigh = Color(0xFF212528),
    surfaceContainerHighest = Color(0xFF2A2E31),
    outline = Color(0xFF7C8183),
    outlineVariant = Color(0xFF34393B),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun MarkPointTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
