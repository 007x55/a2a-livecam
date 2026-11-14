package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SpeedCamColorScheme = darkColorScheme(
    primary = BrightTeal,
    onPrimary = ObsidianBlack,
    secondary = ElectricCyan,
    onSecondary = ObsidianBlack,
    tertiary = RecRedBright,
    background = ObsidianBlack,
    onBackground = TextPrimaryWhite,
    surface = DarkChromeBg,
    onSurface = TextPrimaryWhite,
    surfaceVariant = ControlButtonBg,
    onSurfaceVariant = TextSecondaryGray,
    error = RecRedBright
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpeedCamColorScheme,
        typography = Typography,
        content = content
    )
}
