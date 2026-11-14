package dev.mrbean.laundryledger.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Success = Color(0xFF2E9E5B)
val SuccessContainer = Color(0xFFDDF5E6)
val Warning = Color(0xFFE0782B)

private val Light = lightColorScheme(
    primary = Color(0xFF3B6CE8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0C2A6B),
    secondary = Color(0xFF5B6B8C),
    secondaryContainer = Color(0xFFE3E9F7),
    onSecondaryContainer = Color(0xFF1B2740),
    tertiary = Success,
    background = Color(0xFFF6F8FE),
    surface = Color(0xFFF6F8FE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F3FC),
    surfaceContainer = Color(0xFFEAEFFA),
    surfaceContainerHigh = Color(0xFFE3E9F7),
    surfaceContainerHighest = Color(0xFFDCE3F3),
    outlineVariant = Color(0xFFD5DCEC),
    error = Color(0xFFD64545),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9DB8FF),
    onPrimary = Color(0xFF0B2A6E),
    primaryContainer = Color(0xFF26437F),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondaryContainer = Color(0xFF2C3446),
    tertiary = Color(0xFF6FD69A),
    background = Color(0xFF10131A),
    surface = Color(0xFF10131A),
    surfaceContainerLowest = Color(0xFF1B202B),
    surfaceContainerLow = Color(0xFF181C25),
    surfaceContainer = Color(0xFF262C38),
    surfaceContainerHigh = Color(0xFF2C3240),
    surfaceContainerHighest = Color(0xFF313744),
)

@Composable
fun LaundryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(26.dp),
        ),
        content = content,
    )
}
