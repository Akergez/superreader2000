package com.akergez.superreader2000.shared.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader

// Function to get macOS accent color
private fun getMacOSAccentColor(): Color {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("defaults", "read", "-g", "AppleAccentColor"))
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readLine()
        process.waitFor()

        // Map macOS accent color codes to colors
        // -1 = Graphite (gray), 0 = Red, 1 = Orange, 2 = Yellow, 3 = Green, 4 = Blue (default), 5 = Purple, 6 = Pink
        when (output?.toIntOrNull()) {
            -1 -> Color(0xFF8E8E93) // Graphite
            0 -> Color(255, 62, 79) // Red
            1 -> Color(255, 122, 0) // Orange
            2 -> Color(255, 195, 0) // Yellow
            3 -> Color(55, 189, 45) // Green
            5 -> Color(176, 74, 171) // Purple
            6 -> Color(255, 58, 160) // Pink
            else -> Color(0, 124, 255) // Blue (default)
        }
    } catch (_: Exception) {
        Color(0, 124, 255) // Default to blue if detection fails
    }
}

// Function to create darker variant of accent color
private fun Color.darken(factor: Float = 0.7f): Color {
    return Color(
        red = red * factor,
        green = green * factor,
        blue = blue * factor,
        alpha = alpha
    )
}

// Function to create lighter variant of accent color
private fun Color.lighten(factor: Float = 0.9f): Color {
    return Color(
        red = red + (1f - red) * factor,
        green = green + (1f - green) * factor,
        blue = blue + (1f - blue) * factor,
        alpha = alpha
    )
}

// Get the system accent color once
private val accentColor = getMacOSAccentColor()

// macOS-native color schemes using system accent color
private val MacOSDarkColorScheme = darkColorScheme(
    primary = accentColor,
    onPrimary = Color.White,
    primaryContainer = accentColor,
    onPrimaryContainer = Color.White,
    secondary = accentColor,
    onSecondary = Color.White,
    secondaryContainer = accentColor,
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC77700),
    onTertiaryContainer = Color.White,
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE5E5E5),
    surface = Color(0xFF2D2D2D),
    onSurface = Color(0xFFE5E5E5),
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainer = Color(0xFF2D2D2D),
    surfaceContainerHigh = Color(0xFF3A3A3A),
    surfaceContainerHighest = Color(0xFF4A4A4A)
)

private val MacOSLightColorScheme = lightColorScheme(
    primary = accentColor,
    onPrimary = Color.White,
    primaryContainer = accentColor,
    onPrimaryContainer = Color.White,
    secondary = accentColor,
    onSecondary = Color.White,
    secondaryContainer = accentColor,
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFFF9500),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE5CC),
    onTertiaryContainer = Color(0xFF7A4600),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF48484A),
    surfaceContainer = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color(0xFFE5E5EA),
    surfaceContainerHighest = Color(0xFFD1D1D6)
)

// macOS-native shapes with larger corner radius
private val MacOSShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    content: @Composable (() -> Unit)
) {
    val colorScheme = if (darkTheme) MacOSDarkColorScheme else MacOSLightColorScheme
    val shapes = MacOSShapes

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content
    )
}
